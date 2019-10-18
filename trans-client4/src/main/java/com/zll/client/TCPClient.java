package com.zll.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import com.zll.bean.ServerInfo;
import com.zll.foo.handler.ConnctorStringPacketChain;
import com.zll.foo.handler.ConnectorHandler;
import com.zll.lib.link.box.StringReceivePacket;
import com.zll.utils.CloseUtils;

public class TCPClient extends ConnectorHandler {

	public TCPClient(SocketChannel socketChannel, File cachePath, boolean printReceiveString) throws IOException {
		super(socketChannel, cachePath);
		if (printReceiveString) {
			getStringPacketChain().appendLast(new PrintStringPacketChain());
		}
	}

	private static void write(Socket client) throws IOException {
		// 构建键盘输入流
		InputStream in = System.in;
		BufferedReader input = new BufferedReader(new InputStreamReader(in));

		// 得到Socket输出流，并转换为打印流
		OutputStream outputStream = client.getOutputStream();
		PrintStream socketPrintStream = new PrintStream(outputStream);

		do {
			// 键盘读取一行
			String str = input.readLine();
			// 发送到服务器
			socketPrintStream.println(str);

			if ("00bye00".equalsIgnoreCase(str)) {
				break;
			}
		} while (true);
		// 资源释放
		socketPrintStream.close();
	}

	public static TCPClient startWith(ServerInfo info, File cacheDir) throws IOException {
		return startWith(info, cacheDir, true);
	}

	public static TCPClient startWith(ServerInfo info, File cacheDir, boolean printReceiveString) throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		// System.out.println(socketChannel.hashCode());
		// socket.setSoTimeout(3000) ;
		// 连接本地，端口2000；超时时间3000ms
		socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));
		System.out.println("已发起服务器连接，并进入后续流程～");
		System.out.println("客户端信息：" + socketChannel.getLocalAddress().toString());
		System.out.println("服务器信息：" + socketChannel.getRemoteAddress().toString());
		try {
			return new TCPClient(socketChannel, cacheDir, printReceiveString);
		} catch (Exception e) {
			System.out.println("连接异常");
			CloseUtils.close(socketChannel);
		}
		return null;
	}

	/*
	 * protected void onRecivePacket(ReceivePacket receivePacket) {
	 * super.onRecivePacket(receivePacket); if (receivePacket.type() ==
	 * Packet.TYPE_MEMORY_STRING) { String str = (String)
	 * receivePacket.entity(); System.out.println(key.toString() + " : " + str);
	 * } }
	 */

	private class PrintStringPacketChain extends ConnctorStringPacketChain {

		@Override
		protected boolean consume(ConnectorHandler clientHandler, StringReceivePacket model) {
			String str = model.entity();
			System.out.println(str);
			return true;
		}

	}
}
