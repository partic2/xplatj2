package xplatj.javaplat.pursuer.util;

public class Promise<T> {
	public AsyncFunc<T> func;

	public Promise(AsyncFunc<T> func) {
		state = stateReady;
		this.func = func;
		func.onDone = new Runnable() {
			@Override
			public void run() {
				synchronized (state) {
					state = stateCallingDone;
					if (Promise.this.func.fulfilled) {
						if (onFulfill != null || onReject != null) {
							callThenFunc();
							state = stateThenableCalled;
						}
					}
				}
			}
		};
		func.run();
	}

	protected void callThenFunc() {
		Promise<Object> nextPromise = null;
		if (Promise.this.func.fulfilled && onFulfill != null) {
			nextPromise = (Promise<Object>) onFulfill.call(Promise.this.func.result);
		} else if (!Promise.this.func.fulfilled && onReject != null) {
			nextPromise = (Promise<Object>) onReject.call(Promise.this.func.exception);
		}
		if (nextPromise != null) {
			nextPromise.then(new OneArgFunc<Promise<Object>, Object>() {
				@Override
				public Promise<Object> call(Object result) {
					nextFunc.fulfilled = true;
					nextFunc.result = result;
					nextFunc.onDone.run();
					return null;
				}
			}, new OneArgFunc<Promise<Object>, Exception>() {
				@Override
				public Promise<Object> call(Exception e) {
					nextFunc.fulfilled = false;
					nextFunc.exception = e;
					nextFunc.onDone.run();
					return null;
				}
			});
		}
	}

	public OneArgFunc<Promise<Object>, T> onFulfill;
	public OneArgFunc<Promise<Object>, Exception> onReject;
	public AsyncFunc<Object> nextFunc;

	public Integer state = 1;

	public static final int stateReady = 1;
	public static final int stateIncalling = 2;
	public static final int stateCallingDone = 3;
	public static final int stateThenableCalled = 4;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T2> Promise<T2> then(OneArgFunc<Promise<T2>, T> onFulfilled,
			OneArgFunc<Promise<T2>, Exception> onRejected) {
		AsyncFunc<T2> t1 = new AsyncFunc<T2>() {
			@Override
			public void run() {
			}
		};
		synchronized (state) {
			this.onFulfill = (OneArgFunc) onFulfilled;
			this.onReject = (OneArgFunc) onRejected;
			nextFunc = (AsyncFunc<Object>) t1;
			if (state == stateCallingDone) {
				callThenFunc();
			}
		}

		return new Promise<T2>(t1);
	}
}
