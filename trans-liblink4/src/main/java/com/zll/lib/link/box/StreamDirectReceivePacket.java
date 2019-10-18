package com.zll.lib.link.box;

import java.io.OutputStream;

import com.zll.lib.link.core.Packet;
import com.zll.lib.link.core.ReceivePacket;

public class StreamDirectReceivePacket extends ReceivePacket<OutputStream, OutputStream> {

	private OutputStream outputStream;
	
	public StreamDirectReceivePacket(OutputStream outputStream,long len) {
		super(len);
		// 用以读取数据进行输出的输入流 
		this.outputStream = outputStream;
	}

	@Override
	protected OutputStream buildEntity(OutputStream stream) {
		return outputStream;
	}

	@Override
	protected OutputStream createStream() {
		return outputStream;
	}

	@Override
	public byte type() {
		return Packet.TYPE_STREAM_DIRECT;
	}

}
