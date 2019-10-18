package com.zll.lib.link.box;

import java.io.InputStream;

import com.zll.lib.link.core.Packet;
import com.zll.lib.link.core.SendPacket;

public class StreamDirectSendPacket extends SendPacket<InputStream> {

	private InputStream inputStream;

	public StreamDirectSendPacket(InputStream inputStream) {
		// 用以读取数据进行输出的输入流
		this.inputStream = inputStream;
		// 长度不固定，所以为最大值
		this.length = MAX_PACKET_SIZE;
	}

	@Override
	protected InputStream createStream() {
		return inputStream;
	}

	@Override
	public byte type() {
		return Packet.TYPE_STREAM_DIRECT;
	}

}
