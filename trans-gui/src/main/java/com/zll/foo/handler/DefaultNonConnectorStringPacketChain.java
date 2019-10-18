package com.zll.foo.handler;

import com.zll.lib.link.box.StringReceivePacket;
/**
 * 默认String接收节点，不做任何事情
 * @author Administrator
 *
 */
public class DefaultNonConnectorStringPacketChain extends ConnctorStringPacketChain {

	@Override
	protected boolean consume(ConnectorHandler clientHandler, StringReceivePacket model) {
		// TODO Auto-generated method stub
		return false;
	}

}
