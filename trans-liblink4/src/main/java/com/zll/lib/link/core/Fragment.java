package com.zll.lib.link.core;

import java.io.IOException;

/**
 * 帧头+body,其中帧头包含6个字节，body最多65535个字节
 * @author Administrator
 *
 */
public abstract class Fragment {

	// 数据分片头部长度 6byte
	public static final int FRAG_HEADER_LENGTH = 6;
	// 单个分片最大容量 2^16-1 KB
	public static final int FRAG_MAX_CAPICITY = 65535;
	// 数据帧类型 
	// packet 头数据片
	public static final byte TYPE_PACKET_HEADER = 11;
	// pakcet 内容数据分片
	public static final byte TYPE_PACKET_ENTITY = 12;
	// 数据片-指令 -发送取消
	public static final byte TYPE_COMMAND_SEND_CANCEL = 41;
	// 数据片-指令-接收拒绝
	public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;
	// 数据片-指令-心跳包 
	public static final byte TYPE_COMMAND_HEARTBEAT = 81;
	// flag标记
	public static final byte FLAG_NONE = 0;

	// 头部6个字节固定
	protected final byte[] header = new byte[FRAG_HEADER_LENGTH];

	public Fragment(int length,byte type,byte flag,short identifier) {
		if(length<0 || length>FRAG_MAX_CAPICITY){
			throw new RuntimeException("The Body length of a single fragment should be between 0 and " + FRAG_MAX_CAPICITY);
		}
		
		if(identifier <1 || identifier > 255){
			throw new RuntimeException("The Body identifier of a single fragment shoud be between 1 and 255");
		}
		
		header[0] = (byte)(length>>8);
		header[1] = (byte)(length);
		
		header[2] = type;
		header[3] = flag;
		
		header[4] = (byte)identifier; 
		header[5] = 0; 
	}	
	
	public Fragment(byte[] header){
		System.arraycopy(header, 0, this.header, 0, FRAG_HEADER_LENGTH);
	}
	
	/*
	 * 获取body的长度 
	 */
	public int getBodyLength(){
		// 00000010 header[0]
		// 11111111 11111111 11111111 00000010 (int)header[0]
		// 00000000 00000000 00000000 11111111 0xff
		// 00000000 00000000 00000000 00000010 (int)header[0] & 0xff 
		return (((int)header[0] & 0xff) << 8) | ((int)header[1] & 0xff);
	}
	
	/*
	 * 获取body的类型 
	 */
	public byte getBodyType(){
		return header[2];
	}
	
	/*
	 * 获取body的flag 
	 */
	public byte getBodyFlag(){
		return header[3];
	}
	
	/*
	 * 获取body的唯一标志
	 */
	public short getBodyIdentifier(){
		return (short)((short)header[4] & 0xff);
	}
	
	/*
	 * 进行数据读或写操作
	 */
	public abstract boolean handle(IoArgs args) throws IOException ;
	
	/*
	 * 基于当前数据分片尝试构建下一份待消费的数据分片
	 * 只有当这次数据分片消费完成后才能构建下一次待消费数据片
	 */
	public abstract Fragment nextFrag();
	
	public abstract int getConsumableLength();
	
}
