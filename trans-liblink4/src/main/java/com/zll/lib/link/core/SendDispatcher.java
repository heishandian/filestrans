package com.zll.lib.link.core;

import java.io.Closeable;

/**
 * 发送数据的调度者
 * 缓存所有需要发送的数据，通过队列对数据进行发送 
 * 发送钱实现对数据的基本包装,前面加入包裝 
 * @author Administrator
 *
 */
public interface SendDispatcher extends Closeable  {
	void send(SendPacket packet);
	void cancel(SendPacket packet);
	void sendHeartBeat();
}
