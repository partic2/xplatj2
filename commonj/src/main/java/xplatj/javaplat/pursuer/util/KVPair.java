package xplatj.javaplat.pursuer.util;

import java.util.Map.Entry;

public class KVPair<K,V> implements Entry<K, V> {
	public K key;
	public V value;
	
	public KVPair() {
	}
	
	public KVPair(K k,V v) {
		key=k;
		value=v;
	}
	
	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V value) {
		return this.value=value;
	}

}
