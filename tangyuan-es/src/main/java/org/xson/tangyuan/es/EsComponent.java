package org.xson.tangyuan.es;

import java.util.Map;

import org.xson.tangyuan.ComponentVo;
import org.xson.tangyuan.TangYuanComponent;
import org.xson.tangyuan.TangYuanContainer;
import org.xson.tangyuan.es.datasource.EsSourceManager;
import org.xson.tangyuan.es.executor.EsServiceContextFactory;
import org.xson.tangyuan.es.xml.XmlConfigBuilder;
import org.xson.tangyuan.log.Log;
import org.xson.tangyuan.log.LogFactory;
import org.xson.tangyuan.xml.node.AbstractServiceNode.TangYuanServiceType;

public class EsComponent implements TangYuanComponent {

	private static EsComponent	instance	= new EsComponent();

	private Log					log			= LogFactory.getLog(getClass());

	// private String httpClientResource = null;

	static {
		TangYuanContainer.getInstance().registerContextFactory(TangYuanServiceType.ES, new EsServiceContextFactory());
		// TangYuanContainer.getInstance().registerComponent(new ComponentVo(instance, "es", 40, 40));
		TangYuanContainer.getInstance().registerComponent(new ComponentVo(instance, "es"));
	}

	private EsComponent() {
	}

	public static EsComponent getInstance() {
		return instance;
	}

	// public String getHttpClientResource() {
	// return httpClientResource;
	// }

	/** 设置配置文件 */
	public void config(Map<String, String> properties) {
		// log.info("config setting success...");
		// <config-property name="http.client.resource" value="http.client.properties"/>
		// if (properties.containsKey("http.client.resource".toUpperCase())) {
		// this.httpClientResource = StringUtils.trim(properties.get("http.client.resource".toUpperCase()));
		// }
	}

	public void start(String resource) throws Throwable {
		log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		log.info("elasticsearch component starting, version: " + Version.getVersion());
		XmlConfigBuilder xmlBuilder = new XmlConfigBuilder();
		xmlBuilder.parse(TangYuanContainer.getInstance().getXmlGlobalContext(), resource);
		EsSourceManager.start();
		log.info("elasticsearch component successfully.");
	}

	@Override
	public void stop(boolean wait) {
		EsSourceManager.stop();
		log.info("elasticsearch component stop successfully.");
	}

}
