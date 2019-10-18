package com.zll.lib.link.impl.async;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.zll.lib.link.core.IoArgs;
import com.zll.lib.link.core.IoArgs.IoArgsEventProcessor;
import com.zll.lib.link.core.ReceiveDispatcher;
import com.zll.lib.link.core.ReceivePacket;
import com.zll.lib.link.core.Receiver;
import com.zll.lib.link.utils.CloseUtils;

/**
 * 数据接收成数据分片，然后再转换为packet 包括reciever的注册和对数据的调度
 * 
 * @author Administrator
 *
 */
public class AsyncReceiveDispatcher
		implements ReceiveDispatcher, IoArgsEventProcessor, AsyncFragmentWriter.PacketProvider {

	private final AtomicBoolean isClosed = new AtomicBoolean(false);
	private final Receiver receiver;
	private final RecivePacketCallback callback;
	private final AsyncFragmentWriter writer = new AsyncFragmentWriter(this);

	public AsyncReceiveDispatcher(Receiver receiver, RecivePacketCallback callback) {
		this.receiver = receiver;
		this.receiver.setReceiveListener(this);
		this.callback = callback;
	}

	@Override
	public void start() {
		registerReceive();
	}

	/**
	 * 注册接收数据
	 */
	private void registerReceive() {
		try {
			receiver.postReceiveAsync();
		} catch (Exception e) {
			CloseUtils.close(this);
		}
	}

	@Override
	public void stop() {
		receiver.setReceiveListener(null);
	}

	@Override
	public void close() throws IOException {
		if (isClosed.compareAndSet(false, true)) {
			writer.close();
			receiver.setReceiveListener(null);
		}
	}

	/**
	 * 网络接收数据就绪，此时可以读取数据，需要返回一个容器用于接收数据
	 */
	@Override
	public IoArgs providerIoArgs() {
		IoArgs args = writer.takeIoArgs();
		// 一份新的ioargs需要调用一次开始写入数据的操作
		args.startWriting();
		return args;
	}

	/**
	 * 接收数据失败
	 */
	@Override
	public boolean onConsumeFailed(Throwable e) {
		CloseUtils.close(this);
		return true;
	}

	/**
	 * 接收数据成功
	 */
	@Override
	public boolean onConsumeCompleted(IoArgs args) {
		AtomicBoolean isClosed = this.isClosed;
		AsyncFragmentWriter writer = this.writer;

		// 在消费ioargs数据之前标识数据已经填充完成
		args.finishWriting();
		// 有数据则重复消费
		do {
			writer.consumeIoArgs(args);
		} while (args.remained() && !isClosed.get());
		return !isClosed.get();
	}

	@Override
	public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {
		// TODO Auto-generated method stub
		return callback.onArrivedNewPacket(type, length);
	}

	@Override
	public void completedPacket(ReceivePacket packet, boolean isSucceed) {
		// TODO Auto-generated method stub
		CloseUtils.close(packet);
		callback.onReceivePacketCompleted(packet);
	}

	@Override
	public void onReceivedHeartBeatFragment() {
		// TODO Auto-generated method stub
		callback.onReceivedHeartBeat();
	}
}
