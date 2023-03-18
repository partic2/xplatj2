package xplatj.javaplat.pursuer.util;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;

public class CloseableGroup implements Closeable {
	
	public Collection<Closeable> closeable=new LinkedList<Closeable>();
	
	public <T extends Closeable> T add(T c) {
		this.closeable.add(c);
		return c;
	}
	public <T> T addCanClose(T o) throws NoSuchMethodException, SecurityException {
		addMethod(o.getClass().getMethod("close"),o);
		return o;
	}
	public void addMethod(final Method m,final Object o) {
		closeable.add(new Closeable() {
			@Override
			public void close() throws IOException {
				try {
					m.invoke(o);
				} catch (IllegalAccessException e) {
				} catch (IllegalArgumentException e) {
				} catch (InvocationTargetException e) {
				}
			}
		});
	}
	@Override
	public void close() throws IOException {
		for(Closeable c:closeable) {
			try {
				c.close();
			}catch(IOException e) {}
		}
	}
	public void closeQuietly() {
		try {
			this.close();
		}catch(IOException e) {};
	}

}
