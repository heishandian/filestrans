package com.zll.lib.link.box;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

import com.zll.lib.link.core.ReceivePacket;

/**
 * 字符串接收包 
 * @author Administrator
 *
 */
public class StringReceivePacket extends AbsByteReceivePacket<String>{

	public StringReceivePacket(long len) {
		super(len);
	}

	@Override
	protected String buildEntity(ByteArrayOutputStream stream) {
		return new String(stream.toByteArray());
	}

	@Override
	public byte type() {
		return TYPE_MEMORY_STRING;
	}
	
}
