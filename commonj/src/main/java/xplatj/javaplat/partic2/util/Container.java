package xplatj.javaplat.partic2.util;

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
