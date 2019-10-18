package com.zll.lib.link.core.fragment;

import java.io.IOException;

import com.zll.lib.link.core.Fragment;
import com.zll.lib.link.core.IoArgs;
import com.zll.lib.link.core.SendPacket;

public abstract class AbsSendPacketFragment extends AbsSendFragment {
	
	protected volatile SendPacket<?> packet; 

	public AbsSendPacketFragment(int length, byte type, byte flag, short identifier,SendPacket packet) {
		super(length, type, flag, identifier);
		this.packet = packet;
	}

	@Override
	protected int consumeBody(IoArgs args) throws IOException {
		return 0;
	}

	@Override
	public final synchronized Fragment nextFrag() {
		return packet == null?null:buildNextFragment();
	}

	@Override
	public int getConsumableLength() {
		return 0;
	}
   
	/*
	 * 获取当前对应的发送packet 
	 */
	public synchronized SendPacket getPacket(){
		return packet;
	}
	
	@Override
	public synchronized boolean handle(IoArgs args) throws IOException {
		if(packet == null && !isSending()){
			 // 已取消，并且未发送任何数据，直接返回结束，发送下一个数据分片
			return true;
		}
		return super.handle(args);
	}
	

	
	// true ,没有发送任何数据  
	// 1234， 数据还在队列中，没有出队 表示true
	// 只发送了12，34还没有发送，则 false
	public final synchronized boolean abort(){
		boolean isSending = isSending();
		if(isSending){
			fillDirtyDataOnAbort();
		}
		packet = null;
		return !isSending;
	}

	protected void fillDirtyDataOnAbort() {
	}
	
	protected abstract Fragment buildNextFragment();

}
