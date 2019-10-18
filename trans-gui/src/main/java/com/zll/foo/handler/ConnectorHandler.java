package com.zll.foo.handler;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

import com.zll.Common;
import com.zll.lib.link.box.StringReceivePacket;
import com.zll.lib.link.core.Connector;
import com.zll.lib.link.core.IoContext;
import com.zll.lib.link.core.Packet;
import com.zll.lib.link.core.ReceivePacket;
import com.zll.utils.CloseUtils;

public class ConnectorHandler extends Connector {

	private final String clientInfo;
	private final File cachePath;

	private final ConnectorCloseChain closeChain = new DefauPrintConnectorCloseChain();
	private final ConnctorStringPacketChain stringPacketChain = new DefaultNonConnectorStringPacketChain();

	public ConnectorHandler(SocketChannel socketChannel, File cachePath) throws IOException {

		this.clientInfo = socketChannel.getRemoteAddress().toString();
		this.cachePath = cachePath;
		setup(socketChannel);
	}

	@Override
	public void onChannelClosed(SocketChannel channel) {
		super.onChannelClosed(channel);
		closeChain.handle(this, this);
	};

	public void exit() {
		CloseUtils.close(this);
	}

	@Override
	protected void onRecivePacket(ReceivePacket receivePacket) {
		super.onRecivePacket(receivePacket);
		switch (receivePacket.type()) {
		case Packet.TYPE_MEMORY_STRING: {
			deliveryStringPacket((StringReceivePacket) receivePacket);
			break;
		}
		default: {
			System.out.println("New Packet:" + receivePacket.type() + "-" + receivePacket.length());
		}
		}
		/*
		 * if (receivePacket.type() == Packet.TYPE_MEMORY_STRING) { String str =
		 * (String) receivePacket.entity(); System.out.println(key.toString() +
		 * " : " + str); clientHandlerCallback.onNewMessageArrived(this, str); }
		 */

	}

	/*
	 * 避免阻塞当前的数据读取线程调度，则单独交给另外一个线程进行数据
	 */
	private void deliveryStringPacket(StringReceivePacket receivePacket) {
		IoContext.get().getScheduler().delivery(() -> stringPacketChain.handle(this, receivePacket));
	}

	@Override
	protected File createNewReceiveFile() {
		return Common.createRandomTemp(cachePath);
	}

	/*
	 * public interface ClientHandlerCallback {
	 * 
	 * // 自身关闭通知 void onSelfClosed(ClientHandler handler);
	 * 
	 * 
	 * // 收到消息时通知 void onNewMessageArrived(ClientHandler handler, String msg); }
	 */

	public String getInfo() {
		// TODO Auto-generated method stub
		return clientInfo;
	}

	public ConnctorStringPacketChain getStringPacketChain() {
		// TODO Auto-generated method stub
		return stringPacketChain;
	}

	public ConnectorCloseChain getCloseChain() {
		return closeChain;
	}

}
