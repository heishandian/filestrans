package com.zll.lib.link.core.fragment;

import java.io.IOException;

import com.zll.lib.link.core.Fragment;
import com.zll.lib.link.core.IoArgs;

public abstract class AbsSendFragment extends Fragment {
	// 头部待发送长度
	volatile byte headerRemaining = Fragment.FRAG_HEADER_LENGTH;
	// 负载待发送长度 
	volatile int bodyRemaining;

	AbsSendFragment(int length, byte type, byte flag, short identifier) {
		super(length, type, flag, identifier);
		bodyRemaining = length;
	}
	
	AbsSendFragment(byte[] header) {
		super(header);
	}

	/*
	 * 一次性处理一个数据分片，包括帧头+body,写入对应buffer中
	 * @see com.zll.lib.link.core.Fragment#handle(com.zll.lib.link.core.IoArgs)
	 */
	@Override
	public synchronized boolean handle(IoArgs args) throws IOException {
		try {
			args.limit(headerRemaining + bodyRemaining);
			args.startWriting();
			if (headerRemaining > 0 && args.remained()) {
				headerRemaining -= consumeHeader(args);
			}
			if (headerRemaining == 0 && args.remained() && bodyRemaining > 0) {
				bodyRemaining -= consumeBody(args);
			}
			return headerRemaining == 0 && bodyRemaining == 0;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			args.finishWriting();
		}
	}

	// 消费数据分片body部分
	protected abstract int consumeBody(IoArgs args) throws IOException;

	// 消费数据分片帧头部分 ，将body部分放入socket buffer中
	private byte consumeHeader(IoArgs args) {
		int count = headerRemaining;
		int offset = header.length - count;
		return (byte) args.readFrom(header, offset, count);
	}

	/*
	 * 是否已经处于发送数据中，如果已经发送了部分数据则返回True 只要头部数据已经开始消费，则肯定已经处于发送数据中
	 */
	protected synchronized boolean isSending() {
		return headerRemaining < Fragment.FRAG_HEADER_LENGTH;
	}

	@Override
	public int getConsumableLength() {
		return headerRemaining + bodyRemaining;
	}

}
