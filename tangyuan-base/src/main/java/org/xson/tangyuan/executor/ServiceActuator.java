package org.xson.tangyuan.executor;

import java.util.List;

import org.xson.logging.Log;
import org.xson.logging.LogFactory;
import org.xson.tangyuan.TangYuanContainer;
import org.xson.tangyuan.aop.AopSupport;
import org.xson.tangyuan.aop.AspectVo.PointCut;
import org.xson.tangyuan.mr.MapReduce;
import org.xson.tangyuan.mr.MapReduceHander;
import org.xson.tangyuan.ognl.convert.ParameterConverter;
import org.xson.tangyuan.rpc.RpcProxy;
import org.xson.tangyuan.rpc.RpcServiceNode;
import org.xson.tangyuan.util.TangYuanUtil;
import org.xson.tangyuan.xml.node.AbstractServiceNode;

public class ServiceActuator {

	private static Log									log					= LogFactory.getLog(ServiceActuator.class);
	private static ThreadLocal<ThreadServiceContext>	contextThreadLocal	= new ThreadLocal<ThreadServiceContext>();
	private static ParameterConverter					converter			= new ParameterConverter();
	private static AopSupport							aop					= null;
	private static volatile boolean						running				= true;
	private static boolean								onlyProxy			= false;

	@SuppressWarnings("static-access")
	public static void shutdown() {
		running = false;
		long start = System.currentTimeMillis();
		long maxWaitTimeForShutDown = TangYuanContainer.getInstance().getMaxWaitTimeForShutDown();
		while (ServiceContext.globleCounter.get() > 0) {
			if ((System.currentTimeMillis() - start) > maxWaitTimeForShutDown) {
				log.error("wait for service to close timeout, The current context number " + ServiceContext.globleCounter.get());
				return;
			}
			log.info("waiting for service to close.");
			try {
				Thread.currentThread().sleep(500L);
			} catch (InterruptedException e) {
				log.error("wait for service to close exception", e);
				return;
			}
		}
		log.info("service has been closed normally.");
	}

	public static void openOnlyProxyMode() {
		onlyProxy = true;
	}

	public static void openAop() {
		aop = AopSupport.getInstance();
	}

	/* ================================================================================== */

	/** 检查是否是还有存在的上下文中调用, 用于用户安全的关闭 */
	private static void check() {
		if (running) {
			return;
		}
		boolean existingContext = (null == contextThreadLocal.get()) ? false : true;
		if (!existingContext) {
			throw new ServiceException("The current system is shutting down and no longer handles the new service.");
		}
	}

	/** 方法之前的线程调用 */
	private static ServiceContext begin(boolean isNew) {
		ThreadServiceContext threadContext = contextThreadLocal.get();
		if (null == threadContext) {
			threadContext = new ThreadServiceContext();
			contextThreadLocal.set(threadContext);
		}
		return threadContext.getOrCreate(isNew);
	}

	/** 无异常的结束 */
	private static void onSuccess() {
		ThreadServiceContext threadContext = contextThreadLocal.get();
		if (null != threadContext) {
			ServiceContext context = threadContext.get();
			if (null == context) {
				contextThreadLocal.remove();
				return;
			}
			log.debug("context--. hashCode[" + context.hashCode() + "], context[" + (context.counter - 1) + "]");
			if (--context.counter < 1) {
				Throwable ex = null;
				if (null == context.getExceptionInfo()) {
					try {
						context.finish();// 这里是确定的提交
					} catch (Throwable e) {
						context.finishOnException();
						log.error("tangyuan service commit exception", e);
						ex = e;	// fix bug: 这里的异常需要上抛
					}
				} else {
					context.finishOnException();
				}
				log.debug("close a context. hashCode[" + context.hashCode() + "]");
				context.stopMonitor();// stop monitor
				if (threadContext.recycle()) {
					contextThreadLocal.remove();
				}

				// fix bug: 这里的异常需要上抛
				//				if (null != ex) {
				//					if (ex instanceof ServiceException) {
				//						throw (ServiceException) ex;
				//					}
				//					throw new ServiceException(ex);// 是否需要区分反射异常 TODO
				//				}

				if (null != ex) {
					throw TangYuanUtil.getServiceException(ex);
				}
			}
		} else {
			contextThreadLocal.remove();
		}
	}

	/** 发生异常的结束 */
	private static void onException(Throwable throwable, String serviceURI, AbstractServiceNode service) {
		// 如果当前可以处理,当前处理; 如果当前不能处理,上抛,不做日志输出
		ThreadServiceContext threadContext = contextThreadLocal.get();
		ServiceContext context = threadContext.get();
		if (--context.counter < 1) {	//最后一层
			context.finishOnException();
			log.error("execute service exception: " + serviceURI, throwable);
			log.debug("close a context. hashCode[" + context.hashCode() + "] on exception");
			context.stopMonitor();
			if (threadContext.recycle()) {
				contextThreadLocal.remove();
			}
			// 最后一层抛出的异常
			//			if (throwable instanceof ServiceException) {
			//				throw (ServiceException) throwable;
			//			}
			//			throw new ServiceException("Execute service exception: " + service.getServiceKey(), throwable);
			// 最后一层抛出的异常
			throw TangYuanUtil.getServiceException(throwable);
		} else {
			//			ServiceException ex = null;
			//			try {
			//				context.onException(service.getServiceType(), throwable, "Execute service exception: " + service.getServiceKey());
			//			} catch (ServiceException e) {
			//				ex = e;
			//			}
			//			if (null != ex) {
			//				throw ex;
			//			}
			if (null != service) {
				try {
					context.onException(service.getServiceType(), throwable, "Execute service exception: " + serviceURI);
				} catch (Throwable e) {
					throw TangYuanUtil.getServiceException(e);
				}
			} else {
				// service为空, 则表示未找到服务, 未做任务业务, 仅作日志输出
				log.error("execute service exception: " + serviceURI, throwable);
			}
		}
	}

	private static Object getResult(Object result, boolean ignoreWrapper) {
		if (ignoreWrapper) {
			return result;
		}
		boolean allServiceReturnXCO = TangYuanContainer.getInstance().isAllServiceReturnXCO();
		if (allServiceReturnXCO) {
			return TangYuanUtil.retObjToXco(result);
		}
		return result;
	}

	private static Object getExceptionResult(Throwable e) {
		boolean allServiceReturnXCO = TangYuanContainer.getInstance().isAllServiceReturnXCO();
		if (allServiceReturnXCO) {
			return TangYuanUtil.getExceptionResult(e);
		}
		return null;// 发生错误, 一定是NULL
	}

	/* ================================================================================== */

	/**
	 * 获取服务<br />
	 * a/b|www.xx.com/a/b
	 */
	private static AbstractServiceNode findService(String serviceURL) {
		try {
			// 查询本地服务
			AbstractServiceNode service = TangYuanContainer.getInstance().getService(serviceURL);
			if (null != service) {
				return service;
			}
			// 查询远程服务
			service = TangYuanContainer.getInstance().getDynamicService(serviceURL);
			if (null != service) {
				return service;
			}

			// 处理本地占位URL
			if (null != RpcProxy.getPlaceHolderHandler()) {
				String newServiceURL = RpcProxy.getPlaceHolderHandler().parse(serviceURL);
				if (null != newServiceURL) {
					return TangYuanContainer.getInstance().getService(newServiceURL);
				}
			}

			// 创建新的远程服务
			return createDynamicService(serviceURL);
		} catch (Throwable e) {
			log.error("Invalid service url: " + serviceURL, e);
		}
		return null;
	}

	//	private static Object executeProxy(String serviceURI, Object arg) {
	//		Object result = null;
	//		try {
	//			result = RpcProxy.call(serviceURI, (XCO) arg);
	//		} catch (Throwable e) {
	//			if (e instanceof ServiceException) {
	//				throw (ServiceException) e;
	//			} else {
	//				throw new ServiceException(e);
	//			}
	//		}
	//		return result;
	//	}

	private static AbstractServiceNode createDynamicService(String serviceURL) {

		AbstractServiceNode service = null;

		if (null == aop) {
			service = new RpcServiceNode(serviceURL);
			TangYuanContainer.getInstance().addDynamicService(service);
			return service;
		}

		// 是否是拦截方
		if (aop.isInterceptor(serviceURL)) {
			service = new RpcServiceNode(serviceURL);
			TangYuanContainer.getInstance().addDynamicService(service);
			return service;
		}

		service = new RpcServiceNode(serviceURL);
		// 检查并设置被拦截方
		aop.checkAndsetIntercepted(serviceURL, service);
		TangYuanContainer.getInstance().addDynamicService(service);
		return service;
	}

	/* ================================================================================== */

	public static <T> T execute(String serviceURI, Object arg) throws ServiceException {
		return execute(serviceURI, arg, false);
	}

	@SuppressWarnings("unchecked")
	public static <T> T execute(String serviceURI, Object arg, boolean ignoreWrapper) throws ServiceException {
		// 1. 检查系统是否已经正在关闭中了
		check();

		log.info("actuator service: " + serviceURI);

		// 2. 获取上下文
		ServiceContext context = begin(false);
		//		context.updateMonitor(serviceURI);

		AbstractServiceNode service = null;
		Object result = null;
		Throwable ex = null;
		try {

			service = findService(serviceURI);
			if (null == service) {
				throw new ServiceException("Service does not exist: " + serviceURI);// 上抛异常
			}

			//			if (null != aop) {
			//				aop.execBefore(service, arg, PointCut.BEFORE_CHECK);// 前置切面
			//				aop.execBefore(service, arg, PointCut.BEFORE_ALONE);// 前置切面
			//			}
			//			if (null != aop) {
			//				aop.execBefore(service, arg, PointCut.BEFORE_JOIN);// 前置切面
			//			}

			//			if (onlyProxy) {
			//				return (T) executeProxy(serviceURI, arg); 			// TODO 上下文的处理
			//			}

			// TODO 如果是XCO 需要考虑入参的只读特性??

			Object data = converter.parameterConvert(arg, service.getResultType()); // 如果发生异常, ??
			service.execute(context, data);
			result = service.getResult(context);					// 类型转换时可能发生异常

			if (null != aop) {
				aop.execAfter(service, arg, result, ex, PointCut.AFTER_JOIN);
			}

		} catch (Throwable e) {
			ex = e;
			result = getExceptionResult(e);// 防止异常处理后的返回
		} finally {
			// 新增后置处理
			ServiceException throwEx = null;
			try {
				if (null != ex) {
					onException(ex, serviceURI, service);
				} else {
					onSuccess();
				}
			} catch (ServiceException se) {
				throwEx = se;
				if (null == ex) {
					ex = se;// fix bug: help aop
				}
			}
			if (null != aop) {
				aop.execAfter(service, arg, result, ex, PointCut.AFTER_ALONE);
			}
			if (null != throwEx) {
				throw throwEx;
			}
		}
		return (T) getResult(result, ignoreWrapper);
	}

	/**
	 * 单独环境
	 */
	public static <T> T executeAlone(String serviceURI, Object arg) throws ServiceException {
		return executeAlone(serviceURI, arg, false);
	}

	@SuppressWarnings("unchecked")
	public static <T> T executeAlone(String serviceURI, Object arg, boolean ignoreWrapper) throws ServiceException {
		return (T) executeAlone(serviceURI, arg, false, ignoreWrapper);
	}

	private static Object executeAlone(String serviceURI, Object arg, boolean throwException, boolean ignoreWrapper) throws ServiceException {
		check();// 检查系统是否已经正在关闭中了
		log.info("execute alone service: " + serviceURI);

		ServiceContext context = begin(true);
		//		context.updateMonitor(serviceURI);// monitor
		AbstractServiceNode service = null;
		Object result = null;
		Throwable ex = null;
		try {

			//			if (onlyProxy) {
			//				try {
			//					return executeProxy(serviceURI, arg);
			//				} catch (Throwable e) {
			//					log.error("Execute service exception: " + serviceURI, e);
			//					return getExceptionResult(e);
			//				}
			//			}

			service = findService(serviceURI);
			if (null == service) {
				throw new ServiceException("Service does not exist: " + serviceURI);
			}

			//			if (null != aop) {
			//				try {
			//					aop.execBefore(service, arg, PointCut.BEFORE_CHECK);// 前置切面
			//				} catch (Throwable e) {
			//					log.error("Execute aop exception on: " + serviceURI, e);
			//					return getExceptionResult(e);
			//				}
			//				aop.execBefore(service, arg, PointCut.BEFORE_ALONE);// 前置切面
			//			}
			//			if (null != aop) {
			//				aop.execBefore(service, arg, PointCut.BEFORE_JOIN);// 前置切面
			//			}

			Object data = converter.parameterConvert(arg, service.getResultType());
			service.execute(context, data);
			result = service.getResult(context);// 只有类型转换是发生异常

			if (null != aop) {
				aop.execAfter(service, arg, result, ex, PointCut.AFTER_JOIN);
			}

		} catch (Throwable e) {
			ex = e;
			result = getExceptionResult(e);
		} finally {
			// 新增后置处理
			ServiceException throwEx = null;
			try {
				if (null != ex) {
					onException(ex, serviceURI, service);
				} else {
					onSuccess();
				}
			} catch (ServiceException se) {
				throwEx = se;

				// 防止提交产生新异常
				result = getExceptionResult(se);

				// fix bug: help aop
				if (null == ex) {
					ex = se;
				}
			}
			if (null != aop) {
				aop.execAfter(service, arg, result, ex, PointCut.AFTER_ALONE);
			}
			if (null != throwEx) {
				if (throwException) {
					throw throwEx;
				} else {
					log.error("Execute service exception: " + serviceURI, throwEx);
				}
			}
		}
		return getResult(result, ignoreWrapper);
	}

	/**
	 * 单独环境, 异步执行
	 */
	public static void executeAsync(final String serviceURI, final Object arg) {
		check();
		log.info("execute async service: " + serviceURI);
		TangYuanContainer.getInstance().getThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				execute(serviceURI, arg);
			}
		});
	}

	/* ===================================Map Reduce====================================== */

	public static <T> T executeMapReduce(String serviceURI, List<Object> args, MapReduceHander handler, long timeout) throws ServiceException {
		return executeMapReduce(null, serviceURI, args, handler, timeout);
	}

	public static <T> T executeMapReduce(Object mapReduceContext, String serviceURI, List<Object> args, MapReduceHander handler, long timeout)
			throws ServiceException {
		return executeMapReduce(mapReduceContext, serviceURI, args, handler, timeout, false);
	}

	@SuppressWarnings("unchecked")
	public static <T> T executeMapReduce(Object mapReduceContext, String serviceURI, List<Object> args, MapReduceHander handler, long timeout,
			boolean ignoreWrapper) throws ServiceException {
		check();
		Object result = null;
		try {
			result = MapReduce.execute(mapReduceContext, serviceURI, args, handler, timeout);
		} catch (Throwable e) {
			result = getExceptionResult(e);// 防止异常处理后的返回
		}
		return (T) getResult(result, ignoreWrapper);
	}

	public static <T> T executeMapReduce(List<String> services, List<Object> args, MapReduceHander handler, long timeout) throws ServiceException {
		return executeMapReduce(null, services, args, handler, timeout);
	}

	public static <T> T executeMapReduce(Object mapReduceContext, List<String> services, List<Object> args, MapReduceHander handler, long timeout)
			throws ServiceException {
		return executeMapReduce(mapReduceContext, services, args, handler, timeout, false);
	}

	@SuppressWarnings("unchecked")
	public static <T> T executeMapReduce(Object mapReduceContext, List<String> services, List<Object> args, MapReduceHander handler, long timeout,
			boolean ignoreWrapper) throws ServiceException {
		check();
		Object result = null;
		try {
			result = MapReduce.execute(mapReduceContext, services, args, handler, timeout);
		} catch (Throwable e) {
			result = getExceptionResult(e);// 防止异常处理后的返回
		}
		return (T) getResult(result, ignoreWrapper);
	}
}
