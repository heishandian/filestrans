package com.zll.lib.link.impl.steal;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntFunction;

import com.zll.lib.link.core.IoTask;

/**
 * 窃取调度服务
 * 
 * @author Administrator
 *
 */
public class StealingService {

	// 当任务队列数量低于安全值时，不可窃取
	private final int minSafetyThreshold;

	// 线程集合
	private final StealingSelectorThread[] threads;

	// 对应的任务队列
	private final Queue<IoTask>[] queues;

	// 结束标志
	private volatile boolean isTerminated = false;

	public StealingService(StealingSelectorThread[] threads, int minSafetyThreshold) {
		this.threads = threads;
		this.queues = Arrays.stream(threads).map(StealingSelectorThread::getReadyTaskQueue)
				.toArray((IntFunction<Queue<IoTask>[]>) ArrayBlockingQueue[]::new);
		this.minSafetyThreshold = minSafetyThreshold;
	}

	/**
	 * 窃取一个任务，排除自己，从他人窃取一个任务
	 * 
	 * @param excludedQueue
	 * @return
	 */
	IoTask steal(final Queue<IoTask> excludedQueue) {
		final int minSafetyThreshold = this.minSafetyThreshold;
		final Queue<IoTask>[] queues = this.queues;
		for (Queue<IoTask> queue : queues) {
			if (queue == excludedQueue) {
				continue;
			}
			int size = queue.size();
			if (size > minSafetyThreshold) {
				IoTask poll = queue.poll();
				if (poll != null) {
					return poll;
				}
			}
		}
		return null;
	}

	/*
	 * 获取一个不繁忙的线程
	 */
	public StealingSelectorThread getNotBusyThread() {
		StealingSelectorThread targetThread = null;
		long targetKeyCount = Long.MAX_VALUE;
		for (StealingSelectorThread thread : threads) {
			long registerKeyCount = thread.getSaturatinCapacity();
			if (registerKeyCount != -1 && registerKeyCount < targetKeyCount) {
				targetKeyCount = registerKeyCount;
				targetThread = thread;
			}
		}
		return targetThread;
	}

	/*
	 * 结束操作
	 */
	public void shutdown() {
		if (isTerminated) {
			return;
		}
		isTerminated = true;
		for (StealingSelectorThread thread : threads) {
			thread.exit();
		}
	}

	public boolean isTerminated() {
		return isTerminated;
	}

	/*
	 * 执行一个任务
	 * 
	 * @param task
	 */
	public void execute(IoTask task) {

	}
}
