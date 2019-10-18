package com.zll.lib.link.impl.async;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.zll.lib.link.core.IoArgs;
import com.zll.lib.link.core.IoArgs.IoArgsEventProcessor;
import com.zll.lib.link.core.SendDispatcher;
import com.zll.lib.link.core.SendPacket;
import com.zll.lib.link.core.Sender;
import com.zll.lib.link.impl.exception.EmptyIoArgsException;
import com.zll.lib.link.utils.CloseUtils;

/**
 * 异步发送数据的调度封装
 * 
 * @author Administrator packet ->（解包成） ioargs ->（将ioargs丢个sender） sender
 */
public class AsyncSendDispatcher implements SendDispatcher, IoArgsEventProcessor, AsyncFragmentReader.PacketProvider {

	private final AtomicBoolean isClosed = new AtomicBoolean(false);
	// 是否处于发送过程中，就是处于从队列中将packet取出，解包成ioargs 再传给sender
	private AtomicBoolean isSending = new AtomicBoolean();

	private final Sender sender;
	// 可以阻塞队列 最多执行16个任务
	private final BlockingQueue<SendPacket> queue = new ArrayBlockingQueue<>(16);
	private final AsyncFragmentReader reader = new AsyncFragmentReader(this);

	public AsyncSendDispatcher(Sender sender) {
		this.sender = sender;
		sender.setSendListener(this);
	}

	/**
	 * 发送packet 首先添加到队列，如果当前状态为未启动发送状态则 尝试让reder提取一份packet进行数据发送给
	 * 如果提取数据后reader有数据，则进行异步输出注册
	 */
	@Override
	public void send(SendPacket packet) {
		// put会当队列塞满时阻塞，offer不会
		try {
			queue.put(packet);
			requestSend(false);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 发送心跳帧，将心跳帧放到发送队列进行发送
	 * 
	 * @see com.zll.lib.link.core.SendDispatcher#sendHeartBeat()
	 */
	@Override
	public void sendHeartBeat() {
		if (!queue.isEmpty()) {
			return;
		}
		if (reader.requestSendHeartBeatFragment()) {
			requestSend(false);
		}
	}

	/**
	 * 请求网络进行数据发送
	 * 
	 * @param callFromIoConsume
	 *            标识是否从IO消费流程中获得
	 */
	private void requestSend(boolean callFromIoConsume) {
		synchronized (isSending) {
			final AtomicBoolean isRegisterSending = this.isSending;
			final boolean oldState = isSending.get();

			// oldState是true,同时不是从io消费流程中获得（发送心跳，业务层发送packet）
			// 发送之前，isSending状态应该为false,如果不是在io消费流程中，但发送状态又为true，则直接return
			if (isClosed.get() || (oldState && !callFromIoConsume)) {
				return;
			}

			// 在发送流程中但发送状态为false,则同样是异常状态 
			if (callFromIoConsume && !oldState) {
				throw new IllegalStateException("");
			}

			if (reader.requestTakePacket()) {
				isRegisterSending.set(true);
				try {
					sender.postSendAsync();
				} catch (Exception e) {
					e.printStackTrace();
					CloseUtils.close(this);
				}
			} else {
				isRegisterSending.set(false);
			}
		}
	}

	@Override
	public void cancel(SendPacket packet) {
		boolean ret;
		ret = queue.remove(packet);
		if (ret) {
			packet.cancel();
			return;
		}
		reader.cancel(packet);
	}

	@Override
	public void close() {
		if (isClosed.compareAndSet(false, true)) {
			// reader的关闭操作
			reader.close();
			// 清空队列
			queue.clear();
			synchronized (isSending) {
				isSending.set(false);
			}
		}
	}

	@Override
	public IoArgs providerIoArgs() {
		// 提供一份数据填充到IoArgs中,数据发送使用Ioargs
		return isClosed.get() ? null : reader.fillData();
	}

	@Override
	public boolean onConsumeFailed(Throwable e) {

		if (e instanceof EmptyIoArgsException) {
			requestSend(true);
			// 代表不需要关闭当前链接 
			return false;
		} else {
			CloseUtils.close(this);
			return true;
		}
	}

	/**
	 * 网络发送IoArgs完成回调 方法中进行reader对当前队列的Packet的一个提取，并进行后续的数据发送注册
	 * 
	 * @param args
	 *            IoArgs
	 */
	@Override
	public boolean onConsumeCompleted(IoArgs args) {

		synchronized (isSending) {
			AtomicBoolean isRegisterSending = this.isSending;
			final boolean isRunning = !isClosed.get();
			if (!isRegisterSending.get() && isRunning) {
				throw new IllegalStateException("");
			}

			// 当前是否还需要发送数据 
			isRegisterSending.set(isRunning && reader.requestTakePacket());
			return isRegisterSending.get();

		}
		// requestSend();
	}

	@Override
	public SendPacket takepacket() {
		SendPacket packet = queue.poll();
		if (packet == null) {
			return null;
		}
		if (packet.isCanceled()) {
			// 已取消 不用发送
			return takepacket();
		}
		return packet;
	}

	@Override
	public void completedPacket(SendPacket packet, boolean succeed) {
		CloseUtils.close(packet);
	}

}
