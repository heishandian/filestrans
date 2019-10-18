package com.zll.lib.link.core.fragment;

import java.io.IOException;

import com.zll.lib.link.core.IoArgs;

/**
 * 取消传输数据分片，接收实现
 * @author Administrator
 *
 */
public class CancelReceiveFragment extends AbsReceiveFragment{

	public CancelReceiveFragment(byte[] header) {
		super(header);
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
