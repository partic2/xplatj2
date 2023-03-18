package xplatj.javaplat.pursuer.util.defaultimpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import xplatj.javaplat.pursuer.util.KVPair;

public class DefaultImplMap<K, V> implements Map<K, V> {

	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key)!=null;
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V get(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<K> keySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		Set<java.util.Map.Entry<K, V>> eset=new HashSet<java.util.Map.Entry<K, V>>();
		for(K ek:keySet()){
			KVPair<K, V> pair=new KVPair<K, V>(ek,get(ek));
			eset.add(pair);
		}
		return eset;
	}

}
