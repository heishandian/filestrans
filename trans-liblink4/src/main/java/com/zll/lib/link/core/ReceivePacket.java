package com.zll.lib.link.core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * byte 转换为 文件或字符串
 * 
 * @author Administrator
 *
 */
public abstract class ReceivePacket<Stream extends OutputStream, Entity> extends Packet<Stream> {

	// 定义当前接收包的最终实体
	private Entity entity;

	public ReceivePacket(long len) {
		this.length = len;
	}

	public Entity entity() {
		return entity;
	}

	@Override
	protected final void closeStream(Stream stream) throws IOException {
		super.closeStream(stream);
		entity = buildEntity(stream);
	}

	/*
	 * 根据接收到的流转化为对应的实体
	 */
	protected abstract Entity buildEntity(Stream stream);
}
