package xplatj.javaplat.pursuer.util;

import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

public class ScheduledCustomThreadPoolExecutor implements ScheduledExecutorService {

	protected Timer timer;
	protected ExecutorService wrapExecutor;

	protected ScheduledCustomThreadPoolExecutor() {
	}

	public ScheduledCustomThreadPoolExecutor(ExecutorService wrap) {
		timer = new Timer();
		wrapExecutor = wrap;
	}

	public Future<?> submit(Runnable task) {
		return wrapExecutor.submit(task);
	}

	public <T> Future<T> submit(Runnable task, T result) {
		return wrapExecutor.submit(task, result);
	}

	public <T> Future<T> submit(Callable<T> task) {
		return wrapExecutor.submit(task);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return wrapExecutor.invokeAny(tasks);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return wrapExecutor.invokeAny(tasks, timeout, unit);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return wrapExecutor.invokeAll(tasks);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return wrapExecutor.invokeAll(tasks, timeout, unit);
	}

	public void execute(Runnable command) {
		wrapExecutor.execute(command);
	}

	public void shutdown() {
		timer.cancel();
		timer.purge();
		wrapExecutor.shutdown();
	}

	public List<Runnable> shutdownNow() {
		timer.cancel();
		timer.purge();
		return wrapExecutor.shutdownNow();
	}

	public void purge() {
		timer.purge();
	}

	public boolean isShutdown() {
		return wrapExecutor.isShutdown();
	}

	public boolean isTerminated() {
		return wrapExecutor.isTerminated();
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return wrapExecutor.awaitTermination(timeout, unit);
	}

	private class ExecuteRunnableTask<T> extends TimerTask {
		TaskFuture<T> fut;
		public ExecuteRunnableTask(TaskFuture<T> f) {
			this.fut=f;
		}
		@Override
		public void run() {
			ScheduledCustomThreadPoolExecutor.this.execute(new Runnable() {
				@Override
				public void run() {
					fut.timerTaskCall();
				}
			});
		}
	}

	private class TaskFuture<T> implements ScheduledFuture<T> {
		public ExecuteRunnableTask<T> tt;
		
		public Runnable r;
		public Callable<T> c;
		public boolean done = false;
		public T result;
		public Exception excep;
		public Object notifyOnDone = new Object();
		public boolean cancelled = false;
		public Thread taskThread;
		public boolean oneShot=true;
		
		public TaskFuture() {
			this.tt=new ExecuteRunnableTask<T>(this);
			
		}
		@Override
		public long getDelay(TimeUnit unit) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int compareTo(Delayed o) {
			throw new UnsupportedOperationException();
		}

		
		public void timerTaskCall() {
			taskThread=Thread.currentThread();
			try {
				if (r != null) {
					r.run();
				} else if (c != null) {
					result = c.call();
				}
			} catch (Exception e) {
				excep = e;
				tt.cancel();
			}
			taskThread=null;
			if(oneShot) {
				synchronized (notifyOnDone) {
					done = true;
					notifyOnDone.notifyAll();
				}
			}
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			tt.cancel();
			cancelled = true;
			r = null;
			c = null;
			if(taskThread!=null&&mayInterruptIfRunning) {
				taskThread.interrupt();
			}
			synchronized (notifyOnDone) {
				done = true;
				notifyOnDone.notifyAll();
			}
			return true;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return done;
		}

		private T get(long timeout, TimeUnit unit, boolean neverTimeout)
				throws InterruptedException, ExecutionException, TimeoutException {
			synchronized (notifyOnDone) {
				if (done) {
					if (excep != null) {
						throw new ExecutionException(excep);
					} else {
						return result;
					}
				} else {
					if (neverTimeout) {
						notifyOnDone.wait();
					} else {
						notifyOnDone.wait(unit.toMillis(timeout));
					}
					if (cancelled) {
						throw new CancellationException();
					} else if (excep != null) {
						throw new ExecutionException(excep);
					} else {
						return result;
					}
				}
			}
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			try {
				return get(0, null, true);
			} catch (TimeoutException e) {
				throw new ExecutionException(e);
			}
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return get(timeout, unit, false);
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		TaskFuture<Object> tf = new TaskFuture<Object>();
		tf.r=command;
		timer.schedule(tf.tt, unit.toMillis(delay));
		return tf;
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		TaskFuture<V> tf = new TaskFuture<V>();
		tf.c=callable;
		timer.schedule(tf.tt, unit.toMillis(delay));
		return tf;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		TaskFuture<Object> tf = new TaskFuture<Object>();
		tf.r=command;
		tf.oneShot=false;
		timer.schedule(tf.tt, unit.toMillis(initialDelay), unit.toMillis(period));
		return tf;
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		final TimeUnit unitf = unit;
		final long delayf=delay;
		TaskFuture<Object> tf = new TaskFuture<Object>() {
			public void timerTaskCall() {
				super.timerTaskCall();
				tt=new ExecuteRunnableTask<Object>(this);
				timer.schedule(tt, unitf.toMillis(delayf));
			};
		};
		tf.r=command;
		tf.oneShot=false;
		timer.schedule(tf.tt, unit.toMillis(initialDelay));
		return tf;
	}

}
