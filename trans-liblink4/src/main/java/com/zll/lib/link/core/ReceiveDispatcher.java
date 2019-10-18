package com.zll.lib.link.core;

import java.io.Closeable;

/**
 * 接受的数据调度封装
 * 把一份或多份Ioargs组合成一份Packet
 * @author Administrator
 *
 */
public interface ReceiveDispatcher extends Closeable{

	void start();
	void stop(); 
	
	// 当接收数据完成时，进行回调通知外层
	interface RecivePacketCallback{
		ReceivePacket<?, ?> onArrivedNewPacket(byte type,long length);
		void onReceivePacketCompleted(ReceivePacket packet);
		void onReceivedHeartBeat();
	}
}
