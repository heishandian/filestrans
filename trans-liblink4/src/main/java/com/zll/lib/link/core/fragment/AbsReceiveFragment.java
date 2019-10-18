package com.zll.lib.link.core.fragment;

import java.io.IOException;

import com.zll.lib.link.core.Fragment;
import com.zll.lib.link.core.IoArgs;

public abstract class AbsReceiveFragment extends Fragment {

	// 根据数据分片头部构建数据分片可读区域大小
	// 还没有接收数据的长度 
	volatile int bodyRemaining;

	AbsReceiveFragment(byte[] header) {
		super(header);
		bodyRemaining = getBodyLength();
	}

	/*
	 * 接收数据
	 * @see com.zll.lib.link.core.Fragment#handle(com.zll.lib.link.core.IoArgs)
	 */
	@Override
	public synchronized boolean handle(IoArgs args) throws IOException {

		if (bodyRemaining == 0) {
			// 以读取所有数据
			return true;
		}
		bodyRemaining -= comsumeBody(args);
		return bodyRemaining == 0;
	}

	/*
	 * 不需要下一帧操作，只是根据数据分片头部，接收body部分即可
	 * @see com.zll.lib.link.core.Fragment#nextFrag()
	 */
	@Override
	public Fragment nextFrag() {
		return null;
	}

	/*
	 * 可接受的部分只有body，没有头部，已经接收完
	 */
	@Override
	public int getConsumableLength() {
		return bodyRemaining;
	}

	/*
	 * 消费body部分
	 */
	protected abstract int comsumeBody(IoArgs args) throws IOException;

}
