package com.zll.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.zll.Common;
import com.zll.bean.ServerInfo;
import com.zll.foo.handler.ConnectorCloseChain;
import com.zll.foo.handler.ConnectorHandler;
import com.zll.lib.link.core.Connector;
import com.zll.lib.link.core.IoContext;
import com.zll.lib.link.impl.IoSelectorProvider;
import com.zll.lib.link.impl.IoStealingSelectorProvider;
import com.zll.lib.link.impl.async.SchedulerImpl;
import com.zll.lib.link.utils.CloseUtils;

public class ClientTest {
	private static boolean done;

	// 2000*4/200*1000
	private static final int CLIENT_SIZE = 2000;
	private static final int SEND_THREAD_SIZE = 4;
	private static final int SEND_THREAD_DELAY = 200;

	public static void main(String[] args) throws IOException {

		ServerInfo info = UDPSearcher.searchServer(10000);
		System.out.println("Server:" + info);
		if (info == null) {
			return;
		}

		File cachePath = Common.getCacheDir("client/test");

		// 初始化上下文
		IoContext.setup().ioProvider(new IoStealingSelectorProvider(3)).scheduler(new SchedulerImpl(1)).start();

		// 当前连接数量
		int size = 0;
		final List<TCPClient> tcpClients = new ArrayList<>(CLIENT_SIZE);

		// 关闭时移除
		final class TestcloseChain extends ConnectorCloseChain {
			@Override
			protected boolean consume(ConnectorHandler clientHandler, Connector model) {
				tcpClients.remove(clientHandler);
				if (tcpClients.size() == 0) {
					CloseUtils.close(System.in);
				}
				return false;
			}
		}

		for (int i = 0; i < CLIENT_SIZE; i++) {
			try {
				TCPClient tcpClient = TCPClient.startWith(info, cachePath, false);
				if (tcpClient == null) {
					throw new NullPointerException();
				}
				tcpClient.getCloseChain().appendLast(new TestcloseChain());
				tcpClients.add(tcpClient);
				System.out.println("连接成功：" + (++size));

			} catch (IOException | NullPointerException e) {
				System.out.println("连接异常");
				break;
			}

			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		// System.in.read();

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Runnable runnable = () -> {
			while (!done) {
				TCPClient[] copyClients = tcpClients.toArray(new TCPClient[0]);
				for (TCPClient tcpClient : copyClients) {
					tcpClient.send("Hello~~");
				}

				if (SEND_THREAD_DELAY > 0) {
					try {
						Thread.sleep(SEND_THREAD_DELAY);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};

		List<Thread> threads = new ArrayList<>(SEND_THREAD_SIZE);
		for (int i = 0; i < SEND_THREAD_SIZE; i++) {
			Thread thread = new Thread(runnable);
			thread.start();
			threads.add(thread);
		}
		System.in.read();

		// 等待线程完成
		done = true;

		TCPClient[] copyClients = tcpClients.toArray(new TCPClient[0]);

		for (TCPClient tcpClient : copyClients) {
			tcpClient.exit();
		}
		// 关闭框架线程池
		IoContext.close();
		
		for (Thread thread : threads) {
			try {
				thread.interrupt();
			} catch (Exception ingored) {
				// TODO: handle exception
			}
		}

	}

}
