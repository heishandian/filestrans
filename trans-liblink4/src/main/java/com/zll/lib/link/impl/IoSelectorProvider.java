package com.zll.lib.link.impl;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.zll.lib.link.core.IoProvider;
import com.zll.lib.link.utils.CloseUtils;

public class IoSelectorProvider implements IoProvider {

	private final AtomicBoolean isClosed = new AtomicBoolean(false);

	//
	private final AtomicBoolean inRegInput = new AtomicBoolean(false);
	private final AtomicBoolean inRegOutput = new AtomicBoolean(false);

	private final Selector readSelector;
	private final Selector writeSelector;

	private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
	private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

	private final ExecutorService inputHandlePool;
	private final ExecutorService outputHandlePool;

	public IoSelectorProvider() throws IOException {
		readSelector = Selector.open();
		writeSelector = Selector.open();

		inputHandlePool = Executors.newFixedThreadPool(20, new NameableThreadFactory("IoProvider-Input-Thread-"));
		outputHandlePool = Executors.newFixedThreadPool(20, new NameableThreadFactory("IoProvider-Output-Thread-"));
		// 开始输入输出的监听
		startRead();
		startWrite();
	}

	static class SelectThread extends Thread {

		private final AtomicBoolean isClosed;
		private final AtomicBoolean locker;
		private final Selector selector;
		private final HashMap<SelectionKey, Runnable> callMap;
		private final ExecutorService pool;
		private final int keyOps;

		public SelectThread(String name, AtomicBoolean isClosed, AtomicBoolean locker, Selector selector,
				HashMap<SelectionKey, Runnable> callMap, ExecutorService pool, int keyOps) {
			super(name);
			this.isClosed = isClosed;
			this.locker = locker;
			this.selector = selector;
			this.callMap = callMap;
			this.pool = pool;
			this.keyOps = keyOps;
			this.setPriority(Thread.MAX_PRIORITY);
		}

		@Override
		public void run() {
			super.run();
			AtomicBoolean locker = this.locker;
			Selector selector = this.selector;
			HashMap<SelectionKey, Runnable> callMap = this.callMap;
			ExecutorService pool = this.pool;
			while (!isClosed.get()) {
				try {
					// select（）是阻塞状态，直到有一个channel处于就绪状态，除非是被wakeup方法打断或是进程失败
					if (selector.select() == 0) {
						/*
						 * 注册的时候先要wakup，此时返回select()==0,排除这种状态
						 */
						waitSelection(locker);
						continue;
					} else if (locker.get()) {
						waitSelection(locker);
					}

					// selector.close();

					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = selectedKeys.iterator();

					/*
					 * 尽量不使用for循环，当某个selectkey被cancel时会触发异常
					 */
					while (iterator.hasNext()) {
						SelectionKey selectionKey = iterator.next();
						if (selectionKey.isValid()) {
							// 此时处于非阻塞状态
							handleSelection(selectionKey, keyOps, callMap, pool, locker);
						}
						iterator.remove();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClosedSelectorException ingored) {
					break;
				}
			}
		}
	}

	private void startRead() {
		Thread thread = new SelectThread("Clink IoSelectorProvider ReadSelector Thread", isClosed, inRegInput,
				readSelector, inputCallbackMap, inputHandlePool, SelectionKey.OP_READ);
		thread.start();
	}

	private void startWrite() {
		Thread thread = new SelectThread("Clink IoSelectorProvider WriteSelector Thread", isClosed, inRegOutput,
				writeSelector, outputCallbackMap, outputHandlePool, SelectionKey.OP_WRITE);
		thread.start();
	}

	@Override
	public void close() throws IOException {
		if (isClosed.compareAndSet(false, true)) {
			inputHandlePool.shutdown();
			outputHandlePool.shutdown();

			inputCallbackMap.clear();
			outputCallbackMap.clear();

			// 关闭操作中已经包括唤醒队列的操作，但要处理一个关闭异常
			CloseUtils.close(readSelector, writeSelector);
		}
	}

	public void register(HandlerProviderCallback callback) throws Exception {
		SelectionKey key;
		if (callback.ops == SelectionKey.OP_READ) {
			key = registerSelection(callback.channel, readSelector, SelectionKey.OP_READ, inRegInput, inputCallbackMap,
					callback);
		} else {
			key = registerSelection(callback.channel, writeSelector, SelectionKey.OP_WRITE, inRegOutput,
					outputCallbackMap, callback);
		}
		if (key == null) {
			throw new IOException("Register error: channel: " + callback.channel + "ops: " + callback.ops);
		}
	}

	@Override
	public void unRegister(SocketChannel channel) {
		unRegisterSelection(channel, readSelector, inputCallbackMap, inRegInput);
		unRegisterSelection(channel, writeSelector, outputCallbackMap, inRegOutput);
	}

	private static void waitSelection(final AtomicBoolean locker) {
		// TODO Auto-generated method stub
		synchronized (locker) {
			if (locker.get()) {
				try {
					locker.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void handleSelection(SelectionKey selectionKey, int keyOps, HashMap<SelectionKey, Runnable> map,
			ExecutorService pool, AtomicBoolean locker) {

		synchronized (locker) {
			try {
				// 取消继续对keyOps的监听，同时unRegisterInput()中对key的cancel操作，会导致更改感兴趣的操作时会抛出异常，因此捕捉这个异常
				selectionKey.interestOps(selectionKey.interestOps() & ~keyOps);
			} catch (CancelledKeyException e) {
				return;
			}

		}
		Runnable runnable = null;
		runnable = map.get(selectionKey);

		if (runnable != null && !pool.isShutdown()) {
			// 异步调度
			pool.execute(runnable);
		}
	}

	private SelectionKey registerSelection(SocketChannel socketChannel, Selector selector, int registerOps,
			AtomicBoolean locker, HashMap<SelectionKey, Runnable> callbackMap, Runnable callback) {
		synchronized (locker) {
			// 设定锁定状态
			locker.set(true);
			try {
				// 注册前需要唤醒当前的selector,让selector不处于阻塞select状态
				selector.wakeup();
				SelectionKey key = null;
				/*
				 * Set<SelectionKey> selectedKeys = selector.selectedKeys();
				 * Iterator<SelectionKey> it = selectedKeys.iterator(); while
				 * (it.hasNext()) { SelectionKey sk = it.next();
				 * System.out.println(sk.toString()); }
				 */
				if (socketChannel.isRegistered()) {
					// 查询是否已经注册 ,如果已经注册就将key取出
					key = socketChannel.keyFor(selector);
					if (key != null) {
						key.interestOps(key.interestOps() | registerOps);
					}
				}

				if (key == null) {
					key = socketChannel.register(selector, registerOps);
					callbackMap.put(key, callback);
				}
				return key;
			} catch (ClosedChannelException | CancelledKeyException | ClosedSelectorException e) {
				return null;
			} finally {
				// 注册结束后解除锁定
				locker.set(false);
				try {
					locker.notify();
				} catch (Exception ignored) {
				}
			}
		}
	}

	private void unRegisterSelection(SocketChannel sc, Selector selector, HashMap<SelectionKey, Runnable> callbackMap,
			AtomicBoolean locker) {
		synchronized (locker) {
			locker.set(true);
			selector.wakeup();
			try {
				if (sc.isRegistered()) {
					SelectionKey key = sc.keyFor(selector);
					if (key != null) {
						// 取消监听
						// ,cancle取消所有注册的事件，selectionKey.interestOps(selectionKey.interestOps()
						// & ~keyOps);可以取消指定的事件监听
						key.cancel();
						callbackMap.remove(key);
					}
				}
			} finally {
				locker.set(false);
				try {
					locker.notifyAll();
				} catch (Exception ignored) {
					// TODO: handle exception
				}

			}

		}

	}

}
