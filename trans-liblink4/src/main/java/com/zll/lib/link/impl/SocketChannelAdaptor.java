package com.zll.lib.link.impl;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import com.zll.lib.link.core.IoArgs;
import com.zll.lib.link.core.IoArgs.IoArgsEventProcessor;
import com.zll.lib.link.core.IoProvider;
import com.zll.lib.link.core.Receiver;
import com.zll.lib.link.core.Sender;
import com.zll.lib.link.impl.exception.EmptyIoArgsException;
import com.zll.lib.link.utils.CloseUtils;

public class SocketChannelAdaptor implements Sender, Receiver, Closeable {

	private final AtomicBoolean isClosed = new AtomicBoolean(false);
	private final SocketChannel channel;
	private final IoProvider ioProvider;
	private final OnChannelStatusChangedListener listener;

	private final AbsProviderCallback inputCallback;
	private final AbsProviderCallback outputCallback;

	public SocketChannelAdaptor(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangedListener listener)
			throws IOException {
		this.channel = channel;
		this.ioProvider = ioProvider;
		this.listener = listener;
		this.inputCallback = new InputProviderCallback(channel, SelectionKey.OP_READ, ioProvider);
		this.outputCallback = new OutputProviderCallback(channel, SelectionKey.OP_WRITE, ioProvider);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		if (isClosed.compareAndSet(false, true)) {
			// 解除注册回调
			ioProvider.unRegister(channel);
			// 关闭
			CloseUtils.close(channel);
			// 回调当前Channel已关闭
			listener.onChannelClosed(channel);
		}
	}

	public interface OnChannelStatusChangedListener {
		void onChannelClosed(SocketChannel channel);
	}

	@Override
	public void postReceiveAsync() throws Exception {
		if (isClosed.get() || !channel.isOpen()) {
			throw new IOException("Current channel is closed!");
		}

		// 进行callback状态检查，判断是否属于内部自循环状态
		inputCallback.checkAttachNull();
		ioProvider.register(inputCallback);
	}

	@Override
	public void setReceiveListener(IoArgsEventProcessor processor) {
		inputCallback.eventPeocessor = processor;
	}

	@Override
	public void postSendAsync() throws Exception {
		if (isClosed.get() || !channel.isOpen()) {
			throw new IOException("Current channel is closed!");
		}
		// 进行callback状态检查，判断是否属于内部自循环状态
		outputCallback.checkAttachNull();
		// outputCallback.run(); //有限发送， **在传递文件时，如果在此直接调用发送文件操作，会报出栈溢出异常。
		ioProvider.register(outputCallback);
		// return ioProvider.registerOutput(channel, outputCallback);
		// return true;
	}

	@Override
	public void setSendListener(IoArgsEventProcessor processor) {
		outputCallback.eventPeocessor = processor;
	}

	abstract class AbsProviderCallback extends IoProvider.HandlerProviderCallback {

		volatile IoArgs.IoArgsEventProcessor eventPeocessor;
		volatile long lastActiveTime = System.currentTimeMillis();

		AbsProviderCallback(SocketChannel channel, int ops, IoProvider ioProvider) {
			super(channel, ops, ioProvider);
		}

		@Override
		protected final boolean canProviderIo(IoArgs args) {
			if (isClosed.get()) {
				return false;
			}
			final IoArgs.IoArgsEventProcessor processor = eventPeocessor;
			if (processor == null) {
				return false;
			}

			lastActiveTime = System.currentTimeMillis();

			if (args == null) {
				// 拿一份新的ioargs
				args = processor.providerIoArgs();
			}

			try {
				// 具体的写入操作
				if (args == null) {
					throw new EmptyIoArgsException("ProviderIoArgs is null.");
				}

				int count = consumeIoArgs(args, channel);

				// 检查是否还有未消费数据，并且 ...直接注册下一次调度 
				// ps1 本次一条数据未消费
				// ps2 需要消费完全所有数据 
				if (args.remained() && (count == 0 || args.isNeedConsumeRemaining())) {
					// 附加当前未消费完的数据 
					attach = args;
					// 再次注册数据发送
					return true;
				} else {
					// 写入完成回调
					return processor.onConsumeCompleted(args);
				}

			} catch (IOException e) {
				if (processor.onConsumeFailed(e)) {
					CloseUtils.close(SocketChannelAdaptor.this);
				}
				return false;
			}
		}

		@Override
		public void fireThrowable(Throwable e) {
			final IoArgs.IoArgsEventProcessor processor = eventPeocessor;
			if (processor == null || processor.onConsumeFailed(e)) {
				CloseUtils.close(SocketChannelAdaptor.this);
			}
		}

		protected abstract int consumeIoArgs(IoArgs args, SocketChannel channel) throws IOException;

	}

	class InputProviderCallback extends AbsProviderCallback {

		InputProviderCallback(SocketChannel channel, int ops, IoProvider ioProvider) {
			super(channel, ops, ioProvider);
		}

		@Override
		protected int consumeIoArgs(IoArgs args, SocketChannel channel) throws IOException {
			return args.readFrom(channel);
		}

	}

	class OutputProviderCallback extends AbsProviderCallback {

		OutputProviderCallback(SocketChannel channel, int ops, IoProvider ioProvider) {
			super(channel, ops, ioProvider);
		}

		@Override
		protected int consumeIoArgs(IoArgs args, SocketChannel channel) throws IOException {
			return args.writeTo(channel);
		}

	}

	@Override
	public long getLastReadTime() {
		return inputCallback.lastActiveTime;
	}

	@Override
	public long getLastWriteTime() {
		return outputCallback.lastActiveTime;
	}

}
