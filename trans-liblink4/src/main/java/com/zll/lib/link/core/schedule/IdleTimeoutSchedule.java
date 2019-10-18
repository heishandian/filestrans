package com.zll.lib.link.core.schedule;

import java.util.concurrent.TimeUnit;

import com.zll.lib.link.core.Connector;
import com.zll.lib.link.core.ScheduleJob;

public class IdleTimeoutSchedule extends ScheduleJob {

	public IdleTimeoutSchedule(long idleTimeout, TimeUnit unit, Connector connector) {
		super(idleTimeout, unit, connector);
	}

	@Override
	public void run() {
		long lastActiveTime = connector.getLastActiveTime();
		long idleTimeoutMilliseconds = this.idleTimeoutMillseconds;
		// 空闲时间 50  当前 100  最后活跃时间 80
		long nextDelay = idleTimeoutMilliseconds - (System.currentTimeMillis() - lastActiveTime);
		
		if(nextDelay <= 0){
			// 超时处理
			schedule(idleTimeoutMilliseconds);
			
			try {
				connector.fireIdleTimeoutEvent();
			} catch (Throwable throwable) {
			    connector.fireExceptionCaught(throwable);	
			}
			
			
		}else{
			// 未超时
			schedule(nextDelay);
		}
	}

}
