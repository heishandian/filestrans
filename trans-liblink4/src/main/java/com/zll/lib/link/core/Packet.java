package com.zll.lib.link.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * 业务层用来发送的数据载体
 * 
 * @author Administrator
 *
 */
public abstract class Packet<Stream extends Closeable> implements Closeable {

	// 最大包大小，5个字节满载组成的long类型
	public static final long MAX_PACKET_SIZE = (((0xFFL) << 32) | ((0xFFL) << 24) | ((0xFFL) << 16) | ((0xFFL) << 8)
			| ((0xFFL)));
	// BYTES类型
	public static final byte TYPE_MEMORY_BYTES = 1;
	// String类型
	public static final byte TYPE_MEMORY_STRING = 2;
	// 文件类型
	public static final byte TYPE_STREAM_FILE = 3;
	// 长链接流类型
	public static final byte TYPE_STREAM_DIRECT = 4;

	// 长度
	protected long length;

	private Stream stream;

	public long length() {
		return length;
	}

	public final Stream open() {
		if (stream == null) {
			stream = createStream();
		}
		return stream;
	}

	/*
	 * 对外的关闭资源操作，如果流处于打开状态应当进行关闭
	 */
	@Override
	public void close() throws IOException {
		if (stream != null) {
			closeStream(stream);
			stream = null;
		}
	}

	protected void closeStream(Stream stream) throws IOException {
		stream.close();
	}

	protected abstract Stream createStream();

	/*
	 * 类型，直接通过方法得到:
	 */
	public abstract byte type();

	/*
	 * 头部额外信息，用于携带额外的校验信息,比如md5校验信息
	 */
	public byte[] headerInfo() {
		return null;
	}

}
