package com.zll.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

import com.zll.constants.UDPConstants;
import com.zll.utils.ByteUtils;

public class UDPProvider {
	private static Provider PROVIDER_INSTANCE;

	static void start(int port) {
		stop();
		String sn = UUID.randomUUID().toString();
		Provider provider = new Provider(sn, port);
		provider.start();
		PROVIDER_INSTANCE = provider;
	}
	
	static void stop() {
		if (PROVIDER_INSTANCE != null) {
			PROVIDER_INSTANCE.exit();
			PROVIDER_INSTANCE = null;
		}

	}

	private static class Provider extends Thread {
		private final byte[] sn;
		private final int port;
		private boolean done = false;
		private DatagramSocket ds = null;

		final byte[] buffer = new byte[128];

		public Provider(String sn, int port) {
			super();
			this.sn = sn.getBytes();
			this.port = port;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();

			System.out.println("UDPProvider started.");

			try {
				ds = new DatagramSocket(UDPConstants.PORT_SERVER);
				DatagramPacket receivePack = new DatagramPacket(buffer, buffer.length);

				while (!done) {

					ds.receive(receivePack);
					String clientIp = receivePack.getAddress().getHostAddress();
					int clientPort = receivePack.getPort();
					int clientDataLen = receivePack.getLength();
					byte[] clientData = receivePack.getData();
					boolean isValid = clientDataLen >= (UDPConstants.HEADER.length + 2 + 4)
							&& ByteUtils.startsWith(clientData, UDPConstants.HEADER);

					System.out.println("UDPProvider receive form ip:" + clientIp + "\tport:" + clientPort
							+ "\tdataValid:" + isValid);

					if (!isValid) {
						// 无效继续
						continue;
					}

					// 解析命令与回送端口

					int index = UDPConstants.HEADER.length;

					short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));
					int responsePort = (((clientData[index++]) << 24) | ((clientData[index++] & 0xff) << 16)
							| ((clientData[index++] & 0xff) << 8) | ((clientData[index] & 0xff)));

					// 判断合法性
					if (cmd == 1 && responsePort > 0) {
						// 构建一份回送数据
						ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
						byteBuffer.put(UDPConstants.HEADER);
						byteBuffer.putShort((short) 2);
						byteBuffer.putInt(port);
						byteBuffer.put(sn);
						int len = byteBuffer.position();
						// 直接根据发送者构建一份回送信息
						DatagramPacket responsePacket = new DatagramPacket(buffer, len, receivePack.getAddress(),
								responsePort);
						ds.send(responsePacket);
						System.out.println(
								"UDPProvider response to:" + clientIp + "\tport:" + responsePort + "\tdataLen:" + len);
					} else {
						System.out.println("UDPProvider receive cmd nonsupport; cmd:" + cmd + "\tport:" + port);
					}
				}
			} catch (IOException ignored) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			} finally {
				close();
			}

			System.out.println("UDPProvider Finished.");
		}

		private void close() {
			if (ds != null) {
				ds.close();
				ds = null;
			}
		}

		/**
		 * 提供结束
		 */
		void exit() {
			done = true;
			close();
		}

	}
}
