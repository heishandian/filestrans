package com.zll.lib.link.core;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
/**
 * 任务的具体执行者
 * @author Administrator
 *
 */
public abstract class ScheduleJob implements Runnable {

	protected final long idleTimeoutMillseconds;
	protected final Connector connector;

	private volatile Scheduler scheduler;
	private volatile ScheduledFuture scheduledFuture;

	public ScheduleJob(long idleTimeout, TimeUnit unit, Connector connector) {
		this.idleTimeoutMillseconds = unit.toMillis(idleTimeout);
		this.connector = connector;
	}

	/*
	 * 具体执行任务的方法抛给子类
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
	}

	synchronized void schedule(Scheduler scheduler) {
		this.scheduler = scheduler;
		schedule(idleTimeoutMillseconds);
	}

	synchronized void unSchedule() {
		if (scheduler != null) {
			scheduler = null;
		}

		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
			scheduledFuture = null;
		}
	}

	synchronized protected void schedule(long idleTimeoutMillseconds) {
		if (scheduler != null) {
			scheduler.schedule(this, idleTimeoutMillseconds, TimeUnit.MICROSECONDS);
		}
	}

}
