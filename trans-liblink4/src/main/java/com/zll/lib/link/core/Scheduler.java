package com.zll.lib.link.core;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface Scheduler extends Closeable {
	
	/*
	 * 调度一份延迟任务 
	 */
	ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit);

	void delivery(Runnable runnable);
}
