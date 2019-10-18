package com.zll.lib.link.core;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.zll.lib.link.box.BytesReceivePacket;
import com.zll.lib.link.box.FileReceivePacket;
import com.zll.lib.link.box.FileSendPacket;
import com.zll.lib.link.box.StringReceivePacket;
import com.zll.lib.link.box.StringSendPacket;
import com.zll.lib.link.core.ReceiveDispatcher.RecivePacketCallback;
import com.zll.lib.link.impl.SocketChannelAdaptor;
import com.zll.lib.link.impl.async.AsyncReceiveDispatcher;
import com.zll.lib.link.impl.async.AsyncSendDispatcher;
import com.zll.lib.link.utils.CloseUtils;

public abstract class Connector implements Closeable, SocketChannelAdaptor.OnChannelStatusChangedListener {

	protected UUID key = UUID.randomUUID();
	private SocketChannel channel;
	private Sender sender;
	private Receiver receiver;
	private SendDispatcher sendDispatcher;
	private ReceiveDispatcher receiveDispatcher;
	private final List<ScheduleJob> scheduleJobs = new ArrayList<>(4);

	public void setup(SocketChannel socketChannel) throws IOException {
		this.channel = socketChannel;

		socketChannel.configureBlocking(false);
		socketChannel.socket().setSoTimeout(1000);
		socketChannel.socket().setPerformancePreferences(1, 3, 3);
		//单个链接缓冲区大小 
		socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 16 * 1024);
		socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 16 * 1024);
		// keeplive 
		socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		// 复用当前地址 
		socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);

		IoContext context = IoContext.get();

		SocketChannelAdaptor socketChannelAdaptor = new SocketChannelAdaptor(socketChannel, context.getIoProvider(),
				this);
		this.sender = socketChannelAdaptor;
		this.receiver = socketChannelAdaptor;

		sendDispatcher = new AsyncSendDispatcher(sender);
		receiveDispatcher = new AsyncReceiveDispatcher(receiver, receiveCallback);
		// 启动接收
		receiveDispatcher.start();
	}

	public void send(String msg) {
		SendPacket packet = new StringSendPacket(msg);
		// ioargs 与 packet的联系
		sendDispatcher.send(packet);
	}

	public long getLastActiveTime() {
		return Math.max(sender.getLastWriteTime(), receiver.getLastReadTime());
	}

	public void fireIdleTimeoutEvent() {
		sendDispatcher.sendHeartBeat();
	}

	public void schedule(ScheduleJob job) {
		synchronized (scheduleJobs) {
			if (scheduleJobs.contains(job)) {
				return;
			}
			Scheduler scheduler = IoContext.get().getScheduler();
			job.schedule(scheduler);
			scheduleJobs.add(job);
		}
	}

	public void fireExceptionCaught(Throwable throwable) {
		System.out.println("服务端异常！！！");
	}

	// 空闲50s后进行一次检查，之前最后一次时间点

	public void sendFilePacket(SendPacket packet) {
		sendDispatcher.send(packet);
	}

	protected void onRecivePacket(ReceivePacket receivePacket) {
		// System.out.println(key.toString() + ":[New Packet - TYPE]" +
		// receivePacket.type() + ", Length: " + (receivePacket.length/1024) +
		// "KB");
	}

	@Override
	public void onChannelClosed(SocketChannel channel) {
		synchronized (scheduleJobs) {
			for (ScheduleJob scheduleJob : scheduleJobs) {
				scheduleJob.unSchedule();
			}
			scheduleJobs.clear();
		}
		CloseUtils.close(this);
	}

	@Override
	public void close() throws IOException {
		receiveDispatcher.close();
		sendDispatcher.close();
		sender.close();
		receiver.close();
		channel.close();

	}

	protected abstract File createNewReceiveFile();

	private ReceiveDispatcher.RecivePacketCallback receiveCallback = new RecivePacketCallback() {

		@Override
		public void onReceivePacketCompleted(ReceivePacket packet) {
			onRecivePacket(packet);
		}

		@Override
		public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
			switch (type) {
			case Packet.TYPE_MEMORY_BYTES:
				return new BytesReceivePacket(length);
			case Packet.TYPE_MEMORY_STRING:
				return new StringReceivePacket(length);
			case Packet.TYPE_STREAM_FILE:
				return new FileReceivePacket(length, createNewReceiveFile());
			case Packet.TYPE_STREAM_DIRECT:
				return new BytesReceivePacket(length);
			default:
				throw new UnsupportedOperationException("Unsupported packet type: " + type);
			}
		}

		@Override
		public void onReceivedHeartBeat() {
			System.out.println(key.toString() + ":[HeartBeat]");
		}
	};

	public UUID getKey() {
		// TODO Auto-generated method stub
		return key;
	}

}
