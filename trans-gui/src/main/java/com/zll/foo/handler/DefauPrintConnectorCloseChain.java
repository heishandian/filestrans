package com.zll.foo.handler;

import com.zll.lib.link.core.Connector;
/**
 * 关闭连接链式结构
 * @author Administrator
 *
 */
public class DefauPrintConnectorCloseChain extends ConnectorCloseChain {

	@Override
	protected boolean consume(ConnectorHandler clientHandler, Connector model) {
		System.out.println(clientHandler.getInfo() + "EXIT!!,key: " + clientHandler.getKey().toString());
		return false;
	}

}
