package com.zll.lib.link.impl.async;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.zll.lib.link.core.Scheduler;
import com.zll.lib.link.impl.IoSelectorProvider;
import com.zll.lib.link.impl.NameableThreadFactory;

public class SchedulerImpl implements Scheduler {

	private final ScheduledExecutorService scheduledExecutorService;

	private final ExecutorService deliveryPool;

	public SchedulerImpl(int poolSize) {
		
		this.scheduledExecutorService = Executors.newScheduledThreadPool(poolSize,
				new NameableThreadFactory("Scheduler-Thread-"));

		this.deliveryPool = Executors.newFixedThreadPool(4, new NameableThreadFactory("Delivery-Thread-"));
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
		// 调用一个延迟任务
		return scheduledExecutorService.schedule(runnable, delay, unit);
	}

	@Override
	public void close() throws IOException {
		scheduledExecutorService.shutdownNow();
		deliveryPool.shutdownNow();
	}

	@Override
	public void delivery(Runnable runnable) {
		// TODO Auto-generated method stub
		deliveryPool.execute(runnable);
	}

}
