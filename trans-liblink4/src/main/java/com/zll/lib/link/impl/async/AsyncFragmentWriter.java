package com.zll.lib.link.impl.async;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;

import com.zll.lib.link.core.Fragment;
import com.zll.lib.link.core.IoArgs;
import com.zll.lib.link.core.ReceivePacket;
import com.zll.lib.link.core.fragment.AbsReceiveFragment;
import com.zll.lib.link.core.fragment.CancelReceiveFragment;
import com.zll.lib.link.core.fragment.HeartBeatReceiveFragment;
import com.zll.lib.link.core.fragment.ReceiveEntityFragment;
import com.zll.lib.link.core.fragment.ReceiveFragmentFactory;
import com.zll.lib.link.core.fragment.ReceiveHeaderFragment;

public class AsyncFragmentWriter implements Closeable {

	private final PacketProvider provider;
	private final HashMap<Short, PacketModel> packetMap = new HashMap<>();
	private final IoArgs args = new IoArgs();
	private volatile Fragment fragmentTemp;

	public AsyncFragmentWriter(PacketProvider provider) {
		this.provider = provider;
	}

	/*
	 * 构建一份数据容纳封装 当前帧如果没有则返回至少6字节长度的IoArgs， 如果当前帧有，则返回当前帧未消费完成的区间
	 *
	 * @return IoArgs
	 */
	synchronized IoArgs takeIoArgs() {
		args.limit(fragmentTemp == null ? Fragment.FRAG_HEADER_LENGTH : fragmentTemp.getConsumableLength());
		return args;
	}

	/*
	 * 消费IoArgs中的数据
	 *
	 * @param args IoArgs
	 */
	synchronized void consumeIoArgs(IoArgs args) {
		if (fragmentTemp == null) {
			Fragment temp;
			do {
				// 还有未消费数据，则重复构建帧
				temp = buildNewFrame(args);
			} while (temp == null && args.remained());

			if (temp == null) {
				// 最终消费数据完成，但没有可消费区间，则直接返回
				return;
			}

			fragmentTemp = temp;
			if (!args.remained()) {
				// 没有数据，则直接返回
				return;
			}
		}
		// 确保此时currentFrame一定不为null
		Fragment currentFragment = fragmentTemp;
		do {
			try {
				if (currentFragment.handle(args)) {
					// 某帧已接收完成
					if (currentFragment instanceof ReceiveHeaderFragment) {
						// Packet 头帧消费完成，则根据头帧信息构建接收的Packet
						ReceiveHeaderFragment headerFrame = (ReceiveHeaderFragment) currentFragment;
						ReceivePacket packet = provider.takePacket(headerFrame.getPacketType(),
								headerFrame.getPacketLength(), headerFrame.getPacketHeaderInfo());
						appendNewPacket(headerFrame.getBodyIdentifier(), packet);
					} else if (currentFragment instanceof ReceiveEntityFragment) {
						// Packet 实体帧消费完成，则将当前帧消费到Packet
						completeEntityFragment((ReceiveEntityFragment) currentFragment);
					}

					// 接收完成后，直接推出循环，如果还有未消费数据则交给外层调度
					fragmentTemp = null;
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (args.remained());
	}

	/*
	 * 根据args创建新的帧 若当前解析的帧是取消帧，则直接进行取消操作，并返回null
	 *
	 * @param args IoArgs
	 * 
	 * @return 返回新的帧
	 */
	private Fragment buildNewFrame(IoArgs args) {
		AbsReceiveFragment fragment = ReceiveFragmentFactory.createInstance(args);
		if (fragment instanceof CancelReceiveFragment) {
			cancelReceivePacket(fragment.getBodyIdentifier());
			return null;
		} else if (fragment instanceof HeartBeatReceiveFragment) {
			provider.onReceivedHeartBeatFragment();
			return null;
		} else if (fragment instanceof ReceiveEntityFragment) {
			WritableByteChannel channel = getPacketChannel(fragment.getBodyIdentifier());
			((ReceiveEntityFragment) fragment).bindPacketChannel(channel);
		}
		return fragment;
	}

	/*
	 * 当某Packet实体帧消费完成时调用
	 *
	 * @param frame 帧信息
	 */
	private void completeEntityFragment(ReceiveEntityFragment fragment) {
		synchronized (packetMap) {
			short identifier = fragment.getBodyIdentifier();
			int length = fragment.getBodyLength();
			PacketModel model = packetMap.get(identifier);
			if (model == null) {
				return;
			}
			model.unreceiveLength -= length;
			if (model.unreceiveLength <= 0) {
				// 表示接收数据完成
				provider.completedPacket(model.packet, true);
				packetMap.remove(identifier);
			}
		}
	}

	/*
	 * 添加一个新的Packet到当前缓冲区
	 *
	 * @param identifier Packet标志
	 * 
	 * @param packet Packet
	 */
	private void appendNewPacket(short identifier, ReceivePacket packet) {
		synchronized (packetMap) {
			PacketModel model = new PacketModel(packet);
			packetMap.put(identifier, model);
		}
	}

	/*
	 * 获取Packet对应的输出通道，用以设置给帧进行数据传输 因为关闭当前map的原因，可能存在返回NULL
	 *
	 * @param identifier Packet对应的标志
	 * 
	 * @return 通道
	 */
	private WritableByteChannel getPacketChannel(short identifier) {
		synchronized (packetMap) {
			PacketModel model = packetMap.get(identifier);
			return model == null ? null : model.channel;
		}
	}

	/*
	 * 取消某Packet继续接收数据
	 * 
	 * @param identifier Packet标志
	 */
	private void cancelReceivePacket(short identifier) {
		synchronized (packetMap) {
			PacketModel model = packetMap.get(identifier);
			if (model != null) {
				ReceivePacket packet = model.packet;
				provider.completedPacket(packet, false);
			}
		}
	}

	/*
	 * 关闭操作，关闭时若当前还有正在接收的packet,则尝试停止对应的packet接收
	 */
	@Override
	public void close() throws IOException {
		synchronized (packetMap) {
			Collection<PacketModel> values = packetMap.values();
			for (PacketModel value : values) {
				provider.completedPacket(value.packet, false);
			}
			packetMap.clear();
		}
	}

	/**
	 * Packet 提供者
	 */
	interface PacketProvider {
		/*
		 * 拿Packet操作
		 *
		 * @param type Packet类型
		 * 
		 * @param length Packet长度
		 * 
		 * @param headerInfo Packet headerInfo
		 * 
		 * @return 通过类型，长度，描述等信息得到一份接收Packet
		 */
		ReceivePacket takePacket(byte type, long length, byte[] headerInfo);

		void onReceivedHeartBeatFragment();

		/*
		 * 结束一份Packet
		 *
		 * @param packet 接收包
		 * 
		 * @param isSucceed 是否成功接收完成
		 */
		void completedPacket(ReceivePacket packet, boolean isSucceed);
	}

	/**
	 * 用于接受包的简单封装 用以提供packet 通道 未接收数据长度信息存储 用于统计当前packet的接收程度，接收了多少数据分片还剩多少数据分片
	 * 
	 * @author Administrator
	 *
	 */
	static class PacketModel {
		final ReceivePacket packet;
		final WritableByteChannel channel;
		volatile long unreceiveLength;

		public PacketModel(ReceivePacket<?, ?> packet) {
			this.packet = packet;
			this.channel = Channels.newChannel(packet.open());
			this.unreceiveLength = packet.length();
		}
	}

}
