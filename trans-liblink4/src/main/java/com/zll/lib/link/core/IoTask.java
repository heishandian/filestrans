package com.zll.lib.link.core;

import java.nio.channels.SocketChannel;

/**
 * 可用以进行调度的任务封装 任务执行的回调、当前任务类型、任务对应的通道
 * 
 * @author Administrator
 */
public abstract class IoTask {
	public final SocketChannel channel;
	public final int ops;

	public IoTask(SocketChannel channel, int ops) {

		this.channel = channel;
		this.ops = ops;
	}

	public abstract boolean onProcessIo();

	public abstract void fireThrowable(Throwable e);
}
