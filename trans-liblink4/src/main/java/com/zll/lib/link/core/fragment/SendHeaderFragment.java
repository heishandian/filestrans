package com.zll.lib.link.core.fragment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import com.zll.lib.link.core.Fragment;
import com.zll.lib.link.core.IoArgs;
import com.zll.lib.link.core.SendPacket;

/**
 * 首帧，其中body部分包括packet长度，packet type ,packet dataheadinfo等信息
 * @author Administrator
 *
 */
public class SendHeaderFragment extends AbsSendPacketFragment {

	private final byte[] body;
	static final int PACKET_HEADER_FRAGMENT_MIN_LENGTH = 6;

	public SendHeaderFragment(short identifier, SendPacket packet) {
		
		// 构建头帧header部分
		super(PACKET_HEADER_FRAGMENT_MIN_LENGTH, Fragment.TYPE_PACKET_HEADER, Fragment.FLAG_NONE, identifier, packet);
		
		// 构建头帧body部分
		final long packetLength = packet.length();
		final byte packetType = packet.type();
		final byte[] packetHeaderInfo = packet.headerInfo();

		// 头部对应的数据信息长度
		body = new byte[bodyRemaining];
		// 首个数据分片，body前5个字节存储packet长度
		// 00000000 00000000 00000000 00000000 00000000 00000000 00000000
		// 00000100 packetLength
		// 00000000 00000000 00000000 00000000 00000000 header[5]
		body[0] = (byte) (packetLength >> 32);
		body[1] = (byte) (packetLength >> 24);
		body[2] = (byte) (packetLength >> 16);
		body[3] = (byte) (packetLength >> 8);
		body[4] = (byte) packetLength;

		body[5] = packetType;

		if (packetHeaderInfo != null) {
			System.arraycopy(packetHeaderInfo, 0, body, PACKET_HEADER_FRAGMENT_MIN_LENGTH, packetHeaderInfo.length);
		}
	}

	@Override
	protected int consumeBody(IoArgs args) throws IOException {
		int count = bodyRemaining;
		int offset = body.length - count;
		return args.readFrom(body, offset, count);
	}

	@Override
	public Fragment buildNextFragment() {
		InputStream stream = packet.open();
		ReadableByteChannel channel = Channels.newChannel(stream);
		return new SendEntityFragment(getBodyIdentifier(),packet.length(),channel,packet);
	}

	@Override
	public int getConsumableLength() {
		// TODO Auto-generated method stub
		return 0;
	}

}
