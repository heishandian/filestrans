package com.zll.lib.link.impl;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.zll.lib.link.core.IoProvider;
import com.zll.lib.link.core.IoTask;
import com.zll.lib.link.impl.steal.StealingSelectorThread;
import com.zll.lib.link.impl.steal.StealingService;

public class IoStealingSelectorProvider implements IoProvider {

	private final IoStealingThread[] threads;
	private final StealingService stealingService;

	public IoStealingSelectorProvider(int poolsize) throws IOException {
		IoStealingThread[] threads = new IoStealingThread[poolsize];
		for (int i = 0; i < poolsize; i++) {
			Selector selector = Selector.open();
			threads[i] = new IoStealingThread("IoProvider-Thread-" + (i + 1), selector);
		}

		StealingService stealingService = new StealingService(threads, 10);
		for (IoStealingThread thread : threads) {
			thread.setStealingService(stealingService);

			// 提高线程调度效率
			thread.setDaemon(false);
			thread.setPriority(Thread.MAX_PRIORITY);

			thread.start();
		}
		this.threads = threads;
		this.stealingService = stealingService;

	}

	@Override
	public void close() throws IOException {
		stealingService.shutdown();
	}

	@Override
	public void register(HandlerProviderCallback callback) throws Exception {
		StealingSelectorThread thread = stealingService.getNotBusyThread();
		if(thread == null){
			throw new IOException("IoStealingSelectorProvider is shutdown!");
		}
		thread.register(callback);
	}

	@Override
	public void unRegister(SocketChannel channel) {
		// TODO Auto-generated method stub
		if (!channel.isOpen()) {
			// 已关闭，无需解析注册
			return;
		}

		for (IoStealingThread thread : threads) {
			thread.unregister(channel);
		}
	}

	static class IoStealingThread extends StealingSelectorThread {
		public IoStealingThread(String name, Selector selector) {
			super(selector);
			setName(name);
		}

		@Override
		protected boolean processTask(IoTask task) {
			return task.onProcessIo();
		}
	}

}
