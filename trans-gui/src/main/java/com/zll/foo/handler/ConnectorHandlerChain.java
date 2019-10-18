package com.zll.foo.handler;

public abstract class ConnectorHandlerChain<Model> {
	// 当前链式结构必须持有下一个链式结构
	private volatile ConnectorHandlerChain<Model> next;

	public ConnectorHandlerChain<Model> appendLast(ConnectorHandlerChain<Model> newChain) {
		if (newChain == this || this.getClass().equals(newChain.getClass())) {
			return this;
		}

		synchronized (this) {
			if (next == null) {
				next = newChain;
				return newChain;
			}
			return next.appendLast(newChain);
		}
	}

	/**
	 * PS1： 移除节点中的某一个节点及其之后节点 PS2：移除某节点时，如果其具有后续节点，则把后续节点接到当前节点上，实现可以移除中间某个节点
	 * 
	 * @param clx
	 *            移除节点的class信息
	 * @return 是否移除成功
	 */
	public synchronized boolean remove(Class<? extends ConnectorHandlerChain<Model>> clx) {
		// 自己不能移除自己，因为自己未持有上一个链接的引用
		if (this.getClass().equals(clx)) {
			return false;
		}
		synchronized (this) {
			if (next == null) {
				return false;
			} else if (next.getClass().equals(clx)) {
				// 移除next节点
				// a b c ,b移除，a与c要连上
				if (next.next != null) {
					next = next.next;
				} else {
					next = null;
				}
				return true;
			} else {
				return next.remove(clx);
			}
		}
	}

	synchronized boolean handle(ConnectorHandler clientHandler, Model model) {
		ConnectorHandlerChain<Model> next = this.next;
		// 拿下一个节点的操作放在自己消费之前，因为有可能在自己消费的方法里添加新的next节点
		if (consume(clientHandler, model)) {
			return true;
		}

		boolean comsumed = next != null && next.handle(clientHandler, model);

		if (comsumed) {
			return true;
		}
		return consumeAgain(clientHandler, model);
	}

	protected abstract boolean consume(ConnectorHandler clientHandler, Model model);

	protected boolean consumeAgain(ConnectorHandler clientHandler, Model model) {
		return false;
	}
}
