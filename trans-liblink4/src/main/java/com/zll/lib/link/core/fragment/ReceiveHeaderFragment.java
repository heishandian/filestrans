package com.zll.lib.link.core.fragment;

import java.io.IOException;

import com.zll.lib.link.core.IoArgs;

public class ReceiveHeaderFragment extends AbsReceiveFragment {

	private final byte[] body; 
	
	public ReceiveHeaderFragment(byte[] header) {
		super(header);
		// 
		body = new byte[bodyRemaining];
	}
	
	@Override
	protected int comsumeBody(IoArgs args) throws IOException {
		int offset = body.length - bodyRemaining; 
		return args.writeTo(body,offset);
	}

	public long getPacketLength(){
		return ((((long) body[0]) & 0xFFL) << 32)
                | ((((long) body[1]) & 0xFFL) << 24)
                | ((((long) body[2]) & 0xFFL) << 16)
                | ((((long) body[3]) & 0xFFL) << 8)
                | (((long) body[4]) & 0xFFL);
	}
	
	public byte getPacketType(){
		return body[5];
	}
	
	public byte[] getPacketHeaderInfo(){
		if (body.length > SendHeaderFragment.PACKET_HEADER_FRAGMENT_MIN_LENGTH) {
            byte[] headerInfo = new byte[body.length - SendHeaderFragment.PACKET_HEADER_FRAGMENT_MIN_LENGTH];
            System.arraycopy(body, SendHeaderFragment.PACKET_HEADER_FRAGMENT_MIN_LENGTH,
                    headerInfo, 0, headerInfo.length);
            return headerInfo;
        }
        return null;
	}
}
