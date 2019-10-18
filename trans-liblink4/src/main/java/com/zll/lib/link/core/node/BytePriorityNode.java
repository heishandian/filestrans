package com.zll.lib.link.core.node;

/**
 * 
 * @author Administrator 带优先级的节点，可用于构成链表
 * @param <Item>
 */
public class BytePriorityNode<Item> {
	public byte priority;
	public Item item;
	public BytePriorityNode<Item> next;

	public BytePriorityNode(Item item) {
		this.item = item;
	}

	/*
	 * 按优先级追加到当前链表中
	 */
	public void appendWithPriority(BytePriorityNode<Item> node) {
		if (next == null) {
			next = node;
		} else {
			BytePriorityNode<Item> after = this.next;
			if (after.priority < node.priority) {
				// 如果插入节点优先级大于当前下一个节点，则在中间位置插入
				this.next = node;
				node.next = after;
			} else {
				after.appendWithPriority(node);
			}
		}
	}
}
