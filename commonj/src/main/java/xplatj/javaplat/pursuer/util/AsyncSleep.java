package xplatj.javaplat.pursuer.util;

import java.util.concurrent.TimeUnit;

import xplatj.gdxconfig.core.PlatCoreConfig;

public class AsyncSleep<T> extends AsyncFunc<T> {

	public T result;
	public int delay;
	public TimeUnit unit;
	public AsyncSleep(T result,int delay,TimeUnit unit) {
		this.result=result;
		this.delay=delay;
		this.unit=unit;
	}
	@Override
	public void run() {
		PlatCoreConfig.get().executor.schedule(new Runnable() {
			@Override
			public void run() {
				resolve(AsyncSleep.this.result);
			}
		}, delay, unit);
	}

}
