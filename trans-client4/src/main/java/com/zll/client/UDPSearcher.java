package com.zll.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.zll.bean.ServerInfo;
import com.zll.constants.UDPConstants;
import com.zll.utils.ByteUtils;

public class UDPSearcher {
	private static final int LISTEN_PORT = UDPConstants.PORT_CLIENT_RESPONSE;

	public static ServerInfo searchServer(int timeout) {
		// TODO Auto-generated method stub
		System.out.println("UDPSearcher Started.");

		// 成功收到 回送的栅栏
		CountDownLatch receiveLatch = new CountDownLatch(1);
		Listener listener = null;

		try {
			listener = listen(receiveLatch);
			sendBroadcast();
			receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("UDPSearcher Fininshed.");
		if (listener == null) {
			return null;
		}
		List<ServerInfo> devices = listener.getServerAndClose();
		if (devices.size() > 0) {
			return devices.get(0);
		}
		return null;
	}

	private static void sendBroadcast() throws IOException {
		// TODO Auto-generated method stub
		System.out.println("UDPSearcher sendBroadcast started.");

		DatagramSocket ds = new DatagramSocket();

		ByteBuffer bytebuffer = ByteBuffer.allocate(128);
		bytebuffer.put(UDPConstants.HEADER);
		bytebuffer.putShort((short) 1);
		bytebuffer.putInt(LISTEN_PORT);
		DatagramPacket requestPacket = new DatagramPacket(bytebuffer.array(), bytebuffer.position() + 1);
		requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
		requestPacket.setPort(UDPConstants.PORT_SERVER);

		ds.send(requestPacket);
		ds.close();

		// 完成
		System.out.println("UDPSearcher sendBroadcast finished.");
	}

	private static Listener listen(CountDownLatch receiveLatch) throws InterruptedException {
		// TODO Auto-generated method stub
		System.out.println("UDPSearcher start listen.");
		CountDownLatch startDownLatch = new CountDownLatch(1);
		Listener listener = new Listener(LISTEN_PORT, startDownLatch, receiveLatch);
		listener.start();
		startDownLatch.await();
		return listener;
	}

	private static class Listener extends Thread {

		private final int listenerPort;
		private final CountDownLatch startDownLatch;
		private final CountDownLatch receiveLatch;

		private final List<ServerInfo> serverInfoList = new ArrayList<>();
		private final byte[] buffer = new byte[128];
		private final int minLen = UDPConstants.HEADER.length + 2 + 4;

		private boolean done = false;
		private DatagramSocket ds = null;

		public Listener(int listenPort, CountDownLatch startDownLatch, CountDownLatch receiveLatch) {
			// TODO Auto-generated constructor stub
			this.listenerPort = listenPort;
			this.startDownLatch = startDownLatch;
			this.receiveLatch = receiveLatch;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			startDownLatch.countDown();

			try {
				ds = new DatagramSocket(listenerPort);
				DatagramPacket receivepack = new DatagramPacket(buffer, buffer.length);

				while (!done) {

					ds.receive(receivepack);

					String ip = receivepack.getAddress().getHostAddress();
					int port = receivepack.getPort();
					int length = receivepack.getLength();
					byte[] data = receivepack.getData();

					boolean isValid = length >= minLen && ByteUtils.startsWith(data, UDPConstants.HEADER);
					System.out
							.println("UDPSearcher receive form ip:" + ip + "\tport:" + port + "\tdataValid:" + isValid);

					if (!isValid) {
						// 无效继续
						continue;
					}

					ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, UDPConstants.HEADER.length, length);
					final short cmd = byteBuffer.getShort();
					final int serverPort = byteBuffer.getInt();
					if (cmd != 2 || serverPort <= 0) {
						System.out.println("UDPSearcher receive cmd:" + cmd + "\tserverPort:" + serverPort);
						continue;
					}

					String sn = new String(buffer, minLen, length - minLen);
					ServerInfo info = new ServerInfo(serverPort, ip, sn);
					serverInfoList.add(info);
					// 成功接收到一份
					receiveLatch.countDown();

				}
			} catch (Exception e) {

			} finally {
				close();
			}
			System.out.println("UDPSearcher listener finished.");
		}

		private void close() {
			if (ds != null) {
				ds.close();
				ds = null;
			}
		}

		List<ServerInfo> getServerAndClose() {
			done = true;
			close();
			return serverInfoList;
		}

	}
}
