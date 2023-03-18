package xplatj.javaplat.pursuer.util;

public abstract class AsyncFunc<T> implements Runnable{
	public Runnable onDone;
	public T result;
	public Exception exception;
	public boolean fulfilled=false;
	public void resolve(T result) {
		this.result=result;
		this.fulfilled=true;
		onDone.run();
	}
	public void reject(Exception exception) {
		this.exception=exception;
		onDone.run();
	}
}
