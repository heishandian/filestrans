package com.zll.lib.link.core.fragment;

import com.zll.lib.link.core.Fragment;
import com.zll.lib.link.core.IoArgs;

/**
 * 接收分片构建工厂
 * @author Administrator
 *
 */
public class ReceiveFragmentFactory {
	/*
     * 使用传入的帧头数据构建接收帧
     * @param args IoArgs至少需要有6字节数据可读
     * @return 构建的帧头数据
     */
    public static AbsReceiveFragment createInstance(IoArgs args) {
        byte[] buffer = new byte[Fragment.FRAG_HEADER_LENGTH];
        args.writeTo(buffer, 0);
        byte type = buffer[2];
        switch (type) {
            case Fragment.TYPE_COMMAND_SEND_CANCEL:
                return new CancelReceiveFragment(buffer);
            case Fragment.TYPE_PACKET_HEADER:
                return new ReceiveHeaderFragment(buffer);
            case Fragment.TYPE_PACKET_ENTITY:
                return new ReceiveEntityFragment(buffer);
            case Fragment.TYPE_COMMAND_HEARTBEAT:
            	return HeartBeatReceiveFragment.instance;
            default:
                throw new UnsupportedOperationException("Unsupported frame type:" + type);
        }
    }
}
