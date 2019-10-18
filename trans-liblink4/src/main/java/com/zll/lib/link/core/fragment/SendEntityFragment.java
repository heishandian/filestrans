package com.zll.lib.link.core.fragment;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import com.zll.lib.link.core.Fragment;
import com.zll.lib.link.core.IoArgs;
import com.zll.lib.link.core.SendPacket;

public class SendEntityFragment extends AbsSendPacketFragment{
	
	// 通道时复用的，表示一个完整的packet的流形式 ，在每个数据分片中都需要从channel中读取一定长度的数据 
	private final ReadableByteChannel channel;
	
	// 未消费的长度 （消费是指 将channel中的数据变成一个个的数据片）
	// 1234567890
	// 1234 5678 90 
	// 10 4 6， 6 4 2 ，2 2
	private final long unConsumeEntityLength; 
	
	public SendEntityFragment(short identifier,long entityLength,ReadableByteChannel channel,SendPacket packet) {
		super((int)Math.min(entityLength, Fragment.FRAG_MAX_CAPICITY),Fragment.TYPE_PACKET_ENTITY, Fragment.FLAG_NONE, identifier, packet);
		// entityLength packet长度，bodyRemaining当前可以消费的长度 
		this.unConsumeEntityLength = entityLength - bodyRemaining;
		this.channel = channel;
	}

	@Override
	protected int consumeBody(IoArgs args) throws IOException {
		// 用户取消发送，此时channel为空，无法从channel中读取数据 
		if(packet == null){
			// 已终止当前数据分片，则填充假数据 
			return args.fillEmpty(bodyRemaining);
		}
		return args.readFrom(channel);
	}
	@Override
	public Fragment buildNextFragment() {
		if(unConsumeEntityLength == 0){
			return null;
		}
		// 将未消费的长度构建下一个数据分片
		return new SendEntityFragment(getBodyIdentifier(), unConsumeEntityLength, channel, packet);
	}

}
