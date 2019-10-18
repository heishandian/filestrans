package com.zll.lib.link.core;

import java.io.IOException;

public class IoContext {

	private static IoContext INSTANCE;
	// 全局变量，针对所有连接
	private final IoProvider ioProvider;

	private final Scheduler scheduler;

	private IoContext(IoProvider ioProvider, Scheduler scheduler) {
		this.scheduler = scheduler;
		this.ioProvider = ioProvider;
	}

	public IoProvider getIoProvider() {
		return ioProvider;
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	public static IoContext get() {
		return INSTANCE;
	}

	public static StartBoot setup() {
		return new StartBoot();
	}

	public static void close() throws IOException {
		if (INSTANCE != null) {
			INSTANCE.callClose();
		}
	}

	private void callClose() throws IOException {
		ioProvider.close();
		scheduler.close();
	}

	public static class StartBoot {
		private IoProvider ioProvider;
		private Scheduler scheduler;

		private StartBoot() {
		}

		public StartBoot ioProvider(IoProvider ioProvider) {
			this.ioProvider = ioProvider;
			return this;
		}

		public StartBoot scheduler(Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		public IoContext start() {
			INSTANCE = new IoContext(ioProvider, scheduler);
			return INSTANCE;
		}

	}
}
