package com.zll.lib.link.box;

import java.io.ByteArrayOutputStream;

import com.zll.lib.link.core.ReceivePacket;

public abstract class AbsByteReceivePacket<Entity> extends ReceivePacket<ByteArrayOutputStream,Entity> {

	public AbsByteReceivePacket(long len){
		super(len);
	}
	/*
	 * 创建流操作 
	 * 
	 */
	@Override
	protected ByteArrayOutputStream createStream() {
		return new ByteArrayOutputStream((int) length);
	}
}
