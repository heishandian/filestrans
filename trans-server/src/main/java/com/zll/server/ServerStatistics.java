package com.zll.server;

import com.zll.foo.handler.ConnectorHandler;
import com.zll.foo.handler.ConnctorStringPacketChain;
import com.zll.lib.link.box.StringReceivePacket;

public class ServerStatistics {

	long sendSize;
	long recSize;

	ConnctorStringPacketChain statisticsChain() {
		return new StatisticsConnectorStringPacketChain();
	}

	class StatisticsConnectorStringPacketChain extends ConnctorStringPacketChain {

		@Override
		protected boolean consume(ConnectorHandler clientHandler, StringReceivePacket model) {

			System.out.println(clientHandler.getInfo()+ ":" + model.entity().toString());
			// 接收数据自增
			recSize++;
			System.out.println("服务端接收stringPacket数量：  "+ recSize);
			return false;
		}

	}

}
