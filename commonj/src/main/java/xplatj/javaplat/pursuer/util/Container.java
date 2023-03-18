package xplatj.javaplat.pursuer.util;

public class Container<T> {
	protected T content;

	public Container() {
	}

	public Container(T c) {
		content = c;
	}

	public T get() {
		return content;
	}

	public void set(T c) {
		content = c;
	}
}
