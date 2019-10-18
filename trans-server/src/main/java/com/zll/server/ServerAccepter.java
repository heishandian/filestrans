package com.zll.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import com.zll.lib.link.utils.CloseUtils;

public class ServerAccepter extends Thread {
	private boolean done = false;
	private final AccepterListener listener;
	private final CountDownLatch latch = new CountDownLatch(1);
	private final Selector selector;

	public ServerAccepter(AccepterListener listener) throws IOException {
		super("Server-Accepter-Thread");
		this.listener = listener;
		this.selector = Selector.open();
	}

	boolean awaitRunning() {
		try {
			latch.await();
			return true;
		} catch (InterruptedException e) {
			return false;
		}

	}

	@Override
	public void run() {
		super.run();
		// 回调以及该开始进入运行
		latch.countDown();
		Selector selector = this.selector;
		do {
			try {
				if (selector.select() == 0) {
					if (done) {
						break;
					}
					continue;
				}
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				while (iterator.hasNext()) {
					if (done) {
						break;
					}
					SelectionKey keys = iterator.next();
					iterator.remove();
					// 检查当前key的状态是否是我们关注的
					// 客户端到达状态
					if (keys.isAcceptable()) {
						// 服务端建立serversocketchannel等待客户端的连接
						ServerSocketChannel serverSocketChannel = (ServerSocketChannel) keys.channel();
						// socketchannel 表示客户端的一个连接
						SocketChannel socketchannel = serverSocketChannel.accept();

						listener.onNewSocketArrived(socketchannel);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (!done);
		System.out.println("ServerAccepter Finished!");
	}

	void exit() {
		done = true;
		CloseUtils.close(selector);
	}

	interface AccepterListener {
		void onNewSocketArrived(SocketChannel channel);
	}

	public Selector getSelector() {
		return this.selector;
	}
}
