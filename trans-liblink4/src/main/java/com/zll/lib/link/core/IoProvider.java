package com.zll.lib.link.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {

	void register(HandlerProviderCallback callback) throws Exception;

	void unRegister(SocketChannel sc);

	abstract class HandlerProviderCallback extends IoTask implements Runnable {

		private final IoProvider ioProvider;
		protected volatile IoArgs attach;

		public HandlerProviderCallback(SocketChannel channel, int ops, IoProvider ioProvider) {
			super(channel, ops);
			this.ioProvider = ioProvider;
		}

		/*
		 * 可以继续接收或者发送时的回调
		 */
		protected abstract boolean canProviderIo(IoArgs args);

		@Override
		public void run() {

			final IoArgs attach = this.attach;
			this.attach = null;

			if (canProviderIo(attach)) {
				try {
					ioProvider.register(this);
				} catch (Exception e) {
					fireThrowable(e);
				}

			}
		}

		/*
		 * 可以进行接收或者发送时的回调
		 * param : args 可以携带之前的附加值 
		 * 
		 */
		@Override
		public final boolean onProcessIo() {
			final IoArgs attach = this.attach;
			this.attach = null;
			return canProviderIo(attach);
		}
		
		/*
		 * 检查当前的附加值是否为null,如果处于自循环时当前附加值不为null,
		 * 此时如果外层有调度注册异步发送或者接收是错误的
		 */
		public void checkAttachNull() {
			if (attach != null) {
				throw new IllegalStateException("args is not empty !");
			}
		}
	}
}
