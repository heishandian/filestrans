package com.zll.lib.link.impl.async;

import java.io.Closeable;
import java.io.IOException;

import com.zll.lib.link.core.Fragment;
import com.zll.lib.link.core.IoArgs;
import com.zll.lib.link.core.SendPacket;
import com.zll.lib.link.core.fragment.AbsSendPacketFragment;
import com.zll.lib.link.core.fragment.CancelSendFragment;
import com.zll.lib.link.core.fragment.HeartBeatSendFragment;
import com.zll.lib.link.core.fragment.SendEntityFragment;
import com.zll.lib.link.core.fragment.SendHeaderFragment;
import com.zll.lib.link.core.node.BytePriorityNode;

public class AsyncFragmentReader implements Closeable {

	private volatile IoArgs args = new IoArgs();
	private final PacketProvider provider;
	private volatile BytePriorityNode<Fragment> node;
	private volatile int nodeSize = 0;

	// 1 ,2 ,3,4,5....255 ,1 同時可以并發255個packet 存储最后一个标识
	private short lastIdentifier = 0;

	AsyncFragmentReader(PacketProvider provider) {
		this.provider = provider;
	}

	void cancel(SendPacket packet) {
		synchronized (this) {
			if (nodeSize == 0) {
				return;
			}

			for (BytePriorityNode<Fragment> x = node, before = null; x != null; before = x, x = x.next) {
				Fragment fragment = x.item;
				if (fragment instanceof AbsSendPacketFragment) {
					AbsSendPacketFragment packetFragment = (AbsSendPacketFragment) fragment;
					if (packetFragment.getPacket() == packet) {
						boolean removable = packetFragment.abort();
						if (removable) {
							removeFragment(x, before);
							// A B C ,把B移除掉，需要将 A C连上
							if (packetFragment instanceof SendHeaderFragment) {
								// 头部数据分片（头帧），并且未被发送任何数据，直接取消后不需要添加取消发送
								break;
							}
						}

						// 添加终止数据分片，通知到接收方
						CancelSendFragment cancelSendFragment = new CancelSendFragment(
								packetFragment.getBodyIdentifier());
						appendNewFragment(cancelSendFragment);

						// 意外终止，返回失败
						provider.completedPacket(packet, false);
						break;
					}
				}
			}
		}
	}

	/*
	 * 请求获取一个packet，由于senddispatcher实现了AsyncFragmentReader中PacketProvider接口，请求从(
	 * AsyncSendDispatcher中)队列中拿一份packet进行发送， 如果当前reader中有可以用于网络发送的数据，则发送true
	 */
	boolean requestTakePacket() {
		synchronized (this) {
			// 1 代表有数据 ，如果有数据直接返回true，继续发送
			if (nodeSize >= 1) {
				return true;
			}
		}
		SendPacket packet = provider.takepacket();

		if (packet != null) {
			short identifier = generateIdentifier();
			// 创建一个头部数据分片
			SendHeaderFragment sendHeaderFragment = new SendHeaderFragment(identifier, packet);
			appendNewFragment(sendHeaderFragment);
		}
		synchronized (this) {
			return nodeSize != 0;
		}

	}

	interface PacketProvider {

		SendPacket takepacket();

		void completedPacket(SendPacket packet, boolean succeed);
	}

	@Override
	public synchronized void close() {
		// TODO Auto-generated method stub
		while (node != null) {
			Fragment fragment = node.item;
			if (fragment instanceof AbsSendPacketFragment) {
				SendPacket packet = ((AbsSendPacketFragment) fragment).getPacket();
				provider.completedPacket(packet, false);
			}
			node = node.next;
		}
		nodeSize = 0;
		node = null;
	}

	private synchronized void appendNewFragment(Fragment fragment) {
		BytePriorityNode<Fragment> newNode = new BytePriorityNode<>(fragment);
		if (node != null) {
			// 使用优先级别添加到链表
			node.appendWithPriority(newNode);
		} else {
			node = newNode;
		}
		nodeSize++;
	}

	/*
	 * 填充数据到IoArgs中，如果当前有可用于发送的数据分片，则填充数据并返回，如果填充失败则返回null
	 */
	IoArgs fillData() {
		Fragment currentfragment = getCurrentFragment();
		if (currentfragment == null) {
			return null;
		}
		try {
			// 填充数据
			if (currentfragment.handle(args)) {
				// 如果消费完成，则基于当前的数据分片构建下一个数据分片
				Fragment nextFragment = currentfragment.nextFrag();
				if (nextFragment != null) {
					appendNewFragment(nextFragment);
				} else if (currentfragment instanceof SendEntityFragment) {
					// 如果时末尾实体数据分片 ，则通知完成
					provider.completedPacket(((SendEntityFragment) currentfragment).getPacket(), true);
				}
				// 从链表头弹出
				popCurrentFragment();
			}
			return args;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private synchronized Fragment getCurrentFragment() {
		if (node == null) {
			return null;
		}
		return node.item;
	}

	/*
	 * 移除当前数据分片
	 */
	private synchronized void popCurrentFragment() {
		// TODO Auto-generated method stub
		node = node.next;
		nodeSize--;
		if (node == null) {
			requestTakePacket();
		}
	}

	/*
	 * 生成每个packet的唯一标识
	 */
	private short generateIdentifier() {
		short identifier = ++lastIdentifier;
		if (identifier == 255) {
			lastIdentifier = 0;
		}
		return identifier;
	}

	private synchronized void removeFragment(BytePriorityNode<Fragment> removeNode, BytePriorityNode<Fragment> before) {
		//
		if (before == null) {
			// x是头部数据分片
			node = removeNode.next;
		} else {
			before.next = removeNode.next;
		}

		nodeSize--;
		if (node == null) {
			requestTakePacket();
		}
	}

	/*
	 * 请求发送心跳帧
	 * @return 
	 */
	boolean requestSendHeartBeatFragment() {
		for (BytePriorityNode<Fragment> x = node; x != null; x = x.next) {
			Fragment fragment = x.item;
			if (fragment.getBodyType() == Fragment.TYPE_COMMAND_HEARTBEAT) {
				return false;
			}
		}
		appendNewFragment(new HeartBeatSendFragment());
		return true;
	}
}
