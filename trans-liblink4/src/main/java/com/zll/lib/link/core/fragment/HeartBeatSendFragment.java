package com.zll.lib.link.core.fragment;

import java.io.IOException;

import com.zll.lib.link.core.Fragment;
import com.zll.lib.link.core.IoArgs;

/**
 * 心跳包发送帧
 * @author Administrator
 *
 */
public class HeartBeatSendFragment extends AbsSendFragment{

	static final byte[] HEARTBEAT_DATA = new byte[]{0,0,Fragment.TYPE_COMMAND_HEARTBEAT,0,0,0};
	
	public HeartBeatSendFragment() {
		super(HEARTBEAT_DATA);
	}

	@Override
	protected int consumeBody(IoArgs args) throws IOException {
		return 0;
	}

	@Override
	public Fragment nextFrag() {
		return null;
	}

}
