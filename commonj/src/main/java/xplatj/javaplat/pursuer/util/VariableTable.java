package xplatj.javaplat.pursuer.util;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/*
 * VariableTable is an Map<String,Object> aim to store the Context Variables.
 */
public class VariableTable implements Closeable {
	
	/*
	 * Initialize as a copy of parent. If parent is null, create an empty VariableTable
	 */
	public VariableTable(VariableTable parent) {
		if (parent != null) {
			m = new HashMap<String, Object>(parent.get());
		}
		m = new HashMap<String, Object>();
	}

	private Map<String, Object> m;

	private static class RefLink {
		public Class<?> ref;

		public RefLink(Class<?> referent) {
			ref = referent;
		}
	}

	/*
	 * Get a singleton of type cls referred by a SoftReference or WeakReference. If not exist, create one referred by SoftReference and return it.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getSoftSingleton(Class<T> cls) {
		String name = getSoftSingletonKey(cls);
		Object o = m.get(name);
		if (o != null && RefLink.class == o.getClass()) {
			T ret = (T) getSoftSingleton(((RefLink) o).ref);
			return ret;
		} else {
			Reference<T> ret;
			ret = (Reference<T>) m.get(name);
			if (ret == null || ret.get() == null) {
				try {
					try {
						Constructor<T> constr=cls.getDeclaredConstructor();
						ret = new SoftReference<T>(constr.newInstance());
					}catch(NoSuchMethodException e) {
						ret=null;
					}catch(IllegalAccessException e) {
						Constructor<T> constr=cls.getDeclaredConstructor();
						constr.setAccessible(true);
						ret = new SoftReference<T>(constr.newInstance());
					}
				} catch (Exception e) {
					e.printStackTrace();
				} 
				m.put(name, ret);
			}
			return ret.get();
		}
	}
	
	/*
	 * Get a singleton of Class cls referred by a SoftReference or WeakReference. If not exist, create one referred by WeakReference and return it.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getWeakSingleton(Class<T> cls) {
		String name = getSoftSingletonKey(cls);
		Object o = m.get(name);
		if (o != null && RefLink.class == o.getClass()) {
			T ret = (T) getWeakSingleton(((RefLink) o).ref);
			return ret;
		} else {
			Reference<T> ret;
			ret = (Reference<T>) m.get(name);
			if (ret == null || ret.get() == null) {
				try {
					try {
						Constructor<T> constr=cls.getDeclaredConstructor();
						ret = new SoftReference<T>(constr.newInstance());
					}catch(NoSuchMethodException e) {
						ret=null;
					}catch(IllegalAccessException e) {
						Constructor<T> constr=cls.getDeclaredConstructor();
						constr.setAccessible(true);
						ret = new SoftReference<T>(constr.newInstance());
					}
				} catch (Exception e) {
					e.printStackTrace();
				} 
				m.put(name, ret);
			}
			return ret.get();
		}
	}
	
	/*
	 * Get the key of the SoftReference or WeakReference refer to the Class cls.
	 */
	public String getSoftSingletonKey(Class<?> cls){
		String name = cls.getName();
		name = "softref:" + name+".$"+Integer.toHexString(cls.getClassLoader().hashCode());
		return name;
	}
	
	/*
	 * Remove the SoftReference of Class cls.
	 */
	public void removeSoftSingleton(Class<?> cls){
		m.remove(getSoftSingletonKey(cls));
	}

	/*
	 * After calling this method, Calling getSoft/WeakSingleton(from) will equal to calling getSoft/WeakSingleton(to)
	 */
	public void linkSoftSingleton(Class<?> from, Class<?> to) {
		String name = getSoftSingletonKey(from);
		if (to == null) {
			m.remove(name);
		}
		if (from.isAssignableFrom(to)) {
			m.put(name, new RefLink(to));
		} else {
			throw new ClassCastException();
		}
	}
	
	/*
	 * Get the key of the Singleton for the Class cls.
	 */
	public String getHardSingletonKey(Class<?> cls){
		String name = cls.getName();
		name = "hardref:" + name+".$"+Integer.toHexString(cls.getClassLoader().hashCode());;
		return name;
	}
	
	/*
	 * Get a singleton of Class cls.If not exist, return null.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getHardSingleton(Class<T> cls) {
		String name = getHardSingletonKey(cls);
		Object obj = m.get(name);
		T ret;
		if (cls.isInstance(obj)) {
			ret = (T) m.get(name);
		} else {
			ret = null;
		}
		return ret;
	}

	/*
	 * Set a singleton of Class cls.
	 */
	public <T> void setHardSingleton(Class<T> cls, T instance) {
		String name = cls.getName();
		name = getHardSingletonKey(cls);
		m.put(name, instance);
	}

	/*
	 * Get the map of Context Variable.
	 */
	public Map<String, Object> get() {
		return m;
	}

	/*
	 * List all variable.
	 */
	public Iterable<Object> listAllItem(){
		LinkedList<Object> objs=new LinkedList<Object>();
		Iterator<Map.Entry<String, Object>> ite = get().entrySet().iterator();
		while (ite.hasNext()) {
			Map.Entry<String, Object> ee = ite.next();
			if (ee.getValue() instanceof Reference) {
				Object refee = ((Reference<?>) ee.getValue()).get();
				objs.add(refee);
			}else{
				objs.add(ee.getValue());
			}
		}
		return objs;
	}

	@Override
	public void close() throws IOException {
		for(Object ee:listAllItem()){
			if (ee instanceof Closeable) {
				try {
					((Closeable) ee).close();
				} catch (IOException e) {}
			}
		}
	}
}
