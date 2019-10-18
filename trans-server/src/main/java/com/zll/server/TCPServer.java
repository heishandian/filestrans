package com.zll.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.zll.foo.Command;
import com.zll.foo.handler.ConnectorHandler;
import com.zll.foo.handler.ConnctorStringPacketChain;
import com.zll.foo.handler.ConnectorCloseChain;
import com.zll.lib.link.box.StringReceivePacket;
import com.zll.lib.link.core.Connector;
import com.zll.lib.link.core.ScheduleJob;
import com.zll.lib.link.core.schedule.IdleTimeoutSchedule;
import com.zll.lib.link.utils.CloseUtils;
import com.zll.server.ServerAccepter.AccepterListener;

public class TCPServer implements ServerAccepter.AccepterListener, Group.GroupMessageAdapter {

	private final int port;
	private ServerAccepter serverAccepter;
	private final List<ConnectorHandler> clientHandlerList = new ArrayList<>();
	private final File cachePath;
	private ServerSocketChannel server;

	private final Map<String, Group> groups = new HashMap<String, Group>();

	private final ServerStatistics serverStatistics = new ServerStatistics();

	public TCPServer(int PORT_SERVER, File cachePath) {
		this.port = PORT_SERVER;
		this.cachePath = cachePath;
		this.groups.put(Command.TRANS_COMMAND_GROUP_NAME, new Group(Command.TRANS_COMMAND_GROUP_NAME, this));
	}

	public boolean start() {
		try {
			// selector = Selector.open();
			ServerAccepter serverAccepter = new ServerAccepter(this);
			server = ServerSocketChannel.open();
			// 设置为非阻塞
			server.configureBlocking(false);
			// 绑定本地端口
			server.socket().bind(new InetSocketAddress(port));
			// 注册客户端连接到达监听
			server.register(serverAccepter.getSelector(), SelectionKey.OP_ACCEPT);
			this.server = server;
			this.serverAccepter = serverAccepter;
			serverAccepter.start();

			if (serverAccepter.awaitRunning()) {
				System.out.println("服务器准备就绪~");
				System.out.println("服务器信息： " + server.getLocalAddress().toString());
				return true;
			} else {
				System.out.println("启动异常！");
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	void broadcast(String mg) {
		mg = "系统通知：" + mg;

		ConnectorHandler[] clientHandlers;
		synchronized (clientHandlerList) {
			clientHandlers = clientHandlerList.toArray(new ConnectorHandler[0]);
		}

		for (ConnectorHandler clientHandler : clientHandlers) {
			sendMessageToClient(clientHandler, mg);
		}

	}

	@Override
	public void sendMessageToClient(ConnectorHandler handler, String msg) {
		handler.send(msg);
		serverStatistics.sendSize++;
		System.out.println("服务端发送stringPacket数量： " + serverStatistics.sendSize);
	}

	public void stop() {
		if (serverAccepter != null) {
			serverAccepter.exit();
		}

		ConnectorHandler[] clientHandlers;
		synchronized (clientHandlerList) {
			clientHandlers = clientHandlerList.toArray(new ConnectorHandler[0]);
			clientHandlerList.clear();
		}

		for (ConnectorHandler clientHandler : clientHandlers) {
			clientHandler.exit();
		}

		clientHandlerList.clear();

		CloseUtils.close(server);
	}

	/*
	 * @Override public void onNewMessageArrived(ClientHandler handler, String
	 * msg) { // 打印到屏幕 // System.out.println("Received-" +
	 * handler.getClientInfo() + ":" + // msg); // 异步转发消息到客户端
	 * forwardingThreadPoolExecutor.execute(() -> { synchronized
	 * (TCPServer.this) { for (ClientHandler cd : clientHandlerList) { if
	 * (cd.equals(handler)) { // 跳过自己 continue; } // 投递到另外一个线程池发送的 cd.send(msg);
	 * } }
	 * 
	 * }); }
	 */

	/**
	 * 获取当前的状态信息
	 */
	Object[] getStatusString() {
		return new String[] { "客户端数量： " + clientHandlerList.size(), "发送数量： " + serverStatistics.sendSize,
				"接收数量： " + serverStatistics.recSize };
	}

	@Override
	public void onNewSocketArrived(SocketChannel channel) {
		try {
			ConnectorHandler connectorHandler = new ConnectorHandler(channel, cachePath);
			System.out.println(connectorHandler.getInfo() + ": Connected !");
			
			// 添加收到消息的处理责任链
			connectorHandler.getStringPacketChain().appendLast(serverStatistics.statisticsChain())
					.appendLast(new ParseCommandConnectorStringPacketChain());
			// 添加关闭链接时的处理责任链
			connectorHandler.getCloseChain().appendLast(new RemoveQueueOnConnectorCloseChain());

			// 空闲调度任务
			ScheduleJob scheduleJob = new IdleTimeoutSchedule(100, TimeUnit.SECONDS, connectorHandler);
			connectorHandler.schedule(scheduleJob);

			synchronized (clientHandlerList) {
				clientHandlerList.add(connectorHandler);
				System.out.println("当前客户端数量： " + clientHandlerList.size());
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("客户端连接异常： " + e.getMessage());
		}
	}

	private class RemoveQueueOnConnectorCloseChain extends ConnectorCloseChain {
		@Override
		protected boolean consume(ConnectorHandler clientHandler, Connector connector) {
			synchronized (clientHandlerList) {
				clientHandlerList.remove(clientHandler);

				// 移除群聊的客户端
				Group group = groups.get(Command.TRANS_COMMAND_GROUP_NAME);
				group.removeMember(clientHandler);
			}
			return true;
		}

	}

	private class ParseCommandConnectorStringPacketChain extends ConnctorStringPacketChain {

		@Override
		protected boolean consume(ConnectorHandler clientHandler, StringReceivePacket packet) {
			String str = packet.entity();

			if (str.startsWith(Command.TRANS_COMMAND_JOIN_GROUP)) {
				// 加入群
				Group group = groups.get(Command.TRANS_COMMAND_GROUP_NAME);
				if (group.addMember(clientHandler)) {
					sendMessageToClient(clientHandler, "Join Group:" + group.getName());
				}
				return true;
			} else if (str.startsWith(Command.TRANS_COMMAND_LEAVE_GROUP)) {
				// 离开群
				Group group = groups.get(Command.TRANS_COMMAND_GROUP_NAME);
				if (group.removeMember(clientHandler)) {
					sendMessageToClient(clientHandler, "Leave Group:" + group.getName());
				}
				return true;
			}
			return false;
		}

		@Override
		protected boolean consumeAgain(ConnectorHandler clientHandler, StringReceivePacket packet) {
			// 捡漏的模式，当我们第一遍未消费，然后又没有加入到群，自然没有后续的节点消费
			// 此时我们进行二次消费，返回发送过来的消息
			sendMessageToClient(clientHandler, packet.entity());
			return true;
		}

	}

}
