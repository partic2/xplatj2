package xplatj.gdxplat.pursuer.utils.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

public class CollectionUtils {
	public static CollectionUtils instance;

	public static CollectionUtils getInstance() {
		if (instance == null) {
			instance = new CollectionUtils();
		}
		return instance;
	}

	public static CollectionUtils i() {
		return getInstance();
	}

	public <T> Collection<T> intersect(Collection<T> a, Collection<T> b) {
		ArrayList<T> ret = new ArrayList<T>();
		Iterator<T> it = b.iterator();
		while (it.hasNext()) {
			T e = it.next();
			if (a.contains(e)) {
				ret.add(e);
			}
		}
		return ret;
	};

	public <T> Collection<T> union(Collection<T> a, Collection<T> b) {
		Collection<T> ret = new ArrayList<T>();
		ret.addAll(a);
		ret.addAll(b);
		return ret;
	};

	public <T> Collection<T> diff(Collection<T> a, Collection<T> b) {
		ArrayList<T> ret = new ArrayList<T>();
		Collection<T> t;
		t = union(a, b);
		Iterator<T> it = t.iterator();
		while (it.hasNext()) {
			T e = it.next();
			if (!(b.contains(e) && a.contains(e))) {
				ret.add(e);
			}
		}
		return ret;
	};

	public <T> Collection<T> subtract(Collection<T> a, Collection<T> b) {
		ArrayList<T> ret = new ArrayList<T>();
		Iterator<T> it = a.iterator();
		while (it.hasNext()) {
			T e = it.next();
			if (!b.contains(e)) {
				ret.add(e);
			}
		}
		return ret;
	};

}
