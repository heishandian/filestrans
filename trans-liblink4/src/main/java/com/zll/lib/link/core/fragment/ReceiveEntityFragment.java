package com.zll.lib.link.core.fragment;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import com.zll.lib.link.core.IoArgs;

/**
 * 数据分片写入到packet中，packet是基于流，流转化为通道
 * @author Administrator
 *
 */
public class ReceiveEntityFragment extends AbsReceiveFragment {

	private WritableByteChannel channel;

	ReceiveEntityFragment(byte[] header) {
		super(header);
	}

	@Override
	protected int comsumeBody(IoArgs args) throws IOException {
		// write具体写入到packet
		return channel == null ? args.setEmpty(bodyRemaining) : args.writeTo(channel);
	}

	/*
	 * 对外写出的channel
	 */
	public void bindPacketChannel(WritableByteChannel channel) {
		this.channel = channel;
	}

}
