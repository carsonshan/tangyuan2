package org.xson.tangyuan.rpc;

import java.util.Map;

import org.xson.tangyuan.TangYuanComponent;
import org.xson.tangyuan.TangYuanContainer;
import org.xson.tangyuan.log.Log;
import org.xson.tangyuan.log.LogFactory;
import org.xson.tangyuan.rpc.client.AbstractRpcClient;
import org.xson.tangyuan.rpc.xml.XMLConfigBuilder;

/**
 * 先启动, 后关闭
 */
public class RpcClientComponent implements TangYuanComponent {

	private static RpcClientComponent	instance	= new RpcClientComponent();

	private Log							log			= LogFactory.getLog(getClass());

	private AbstractRpcClient			rpcClient	= null;

	private RpcClientComponent() {
	}

	public static RpcClientComponent getInstance() {
		return instance;
	}

	@Override
	public void config(Map<String, String> properties) {
		// RpcContainer.getInstance().config(properties);
	}

	@Override
	public void start(String resource) throws Throwable {
		log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		log.info("rpc client component starting, version: " + Version.getVersion());
		XMLConfigBuilder xmlConfigBuilder = new XMLConfigBuilder();
		xmlConfigBuilder.parse(TangYuanContainer.getInstance().getXmlGlobalContext(), resource);
		log.info("rpc client component successfully.");
	}

	@Override
	public void stop(boolean wait) {
		log.info("rpc client component stopping...");
		if (null != rpcClient) {
			rpcClient.shutdown();
		}
		log.info("rpc client component stop successfully.");
	}

	public void setRpcClient(AbstractRpcClient rpcClient) {
		this.rpcClient = rpcClient;
	}

}
