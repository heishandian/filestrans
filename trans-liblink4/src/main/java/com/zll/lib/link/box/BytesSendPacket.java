package com.zll.lib.link.box;

import java.io.ByteArrayInputStream;

import com.zll.lib.link.core.SendPacket;

/**
 * 按byte数组发送包
 * 
 * @author Administrator
 *
 */
public class BytesSendPacket extends SendPacket<ByteArrayInputStream> {

	private final byte[] bytes;

	public BytesSendPacket(byte[] bytes) {
		this.bytes = bytes;
		this.length = bytes.length;
	}

	@Override
	protected ByteArrayInputStream createStream() {
		return new ByteArrayInputStream(bytes);
	}

	@Override
	public byte type() {
		return TYPE_MEMORY_BYTES;
	}

}
