package com.zll.server;

import java.util.ArrayList;
import java.util.List;

import com.zll.foo.handler.ConnectorHandler;
import com.zll.foo.handler.ConnctorStringPacketChain;
import com.zll.lib.link.box.StringReceivePacket;

class Group {

	private final String name;

	private final List<ConnectorHandler> members = new ArrayList<>();

	private final GroupMessageAdapter adapter;

	public Group(String name, GroupMessageAdapter adapter) {
		this.name = name;
		this.adapter = adapter;
	}

	String getName() {
		return name;
	}

	boolean addMember(ConnectorHandler handler) {
		synchronized (members) {
			if (!members.contains(handler)) {
				members.add(handler);
				handler.getStringPacketChain().appendLast(new ForwardConnctorStringPacket());
				System.out.println("Group[" + name + "] add new member:" + handler.getInfo());
				return true;
			}
		}
		return false;
	}

	boolean removeMember(ConnectorHandler handler) {
		synchronized (members) {
			if (members.remove(handler)) {
				handler.getStringPacketChain().remove(ForwardConnctorStringPacket.class);
				System.out.println("Group[" + name + "] leave member:" + handler.getInfo());
				return true;
			}
		}

		return false;
	}

	private class ForwardConnctorStringPacket extends ConnctorStringPacketChain {

		@Override
		protected boolean consume(ConnectorHandler clientHandler, StringReceivePacket packet) {

			synchronized (members) {
				for (ConnectorHandler mem : members) {
					if (mem == clientHandler) {
						continue;
					}
					adapter.sendMessageToClient(mem, packet.entity());
				}
				return true;
			}

		}
	}

	interface GroupMessageAdapter {
		void sendMessageToClient(ConnectorHandler handler, String msg);
	}
}
