package com.zll.lib.link.core.fragment;

import java.io.IOException;

import com.zll.lib.link.core.Fragment;
import com.zll.lib.link.core.IoArgs;

/**
 * 取消发送数据分片，用于标志某packet取消进行发送数据 
 * @author Administrator
 *
 */
public class CancelSendFragment extends AbsSendFragment{

	// identifier代表我要取消哪个packet
	public CancelSendFragment(short identifier) {
		super(0,Fragment.TYPE_COMMAND_SEND_CANCEL,Fragment.FLAG_NONE,identifier);
	}

	@Override
	protected int consumeBody(IoArgs args) throws IOException {
		return 0;
	}

	@Override
	public Fragment nextFrag() {
		return null;
	}

	@Override
	public int getConsumableLength() {
		// TODO Auto-generated method stub
		return 0;
	}
}
