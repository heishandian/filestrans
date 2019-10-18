package com.zll.lib.link.core.fragment;

import java.io.IOException;

import com.zll.lib.link.core.IoArgs;

/**
 * 心跳包接收帧
 * @author Administrator
 *
 */
public class HeartBeatReceiveFragment extends AbsReceiveFragment{

	static final HeartBeatReceiveFragment instance = new HeartBeatReceiveFragment();
	
	private HeartBeatReceiveFragment() {
		super(HeartBeatSendFragment.HEARTBEAT_DATA);
	}
	
	/*
	 * 取消信息 在数据分片头部，不需要消费body部分
	 * @see com.zll.lib.link.core.fragment.AbsReceiveFragment#comsumeBody(com.zll.lib.link.core.IoArgs)
	 */
	@Override
	protected int comsumeBody(IoArgs args) throws IOException {
		return 0;
	}
}
