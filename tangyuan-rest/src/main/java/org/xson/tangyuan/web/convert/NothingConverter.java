package org.xson.tangyuan.web.convert;

import org.xson.tangyuan.web.DataConverter;
import org.xson.tangyuan.web.RequestContext;
import org.xson.tangyuan.web.xml.vo.ControllerVo;

/**
 * 什么也不做的转换器
 */
public class NothingConverter implements DataConverter {

	public final static NothingConverter instance = new NothingConverter();

	@Override
	public void convert(RequestContext requestContext, ControllerVo cVo) throws Throwable {
	}

}
