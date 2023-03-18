package pursuer.patchedmsgpack.tools;

import pursuer.patchedmsgpack.value.MapValue;
import pursuer.patchedmsgpack.value.Value;
import pursuer.patchedmsgpack.value.ValueFactory;

public class MapBuilder2{
	public ValueFactory.MapBuilder wrap=ValueFactory.newMapBuilder();
	public MapBuilder2 put(String key,Value val) {
		wrap.put(ValueFactory.newString(key),val);
		return this;
	}
	public MapBuilder2 put(String key,String val) {
		put(key,ValueFactory.newString(val));
		return this;
	}
	public MapBuilder2 put(String key,int val) {
		put(key,ValueFactory.newInteger(val));
		return this;
	}
	public MapBuilder2 put(String key,long val) {
		put(key,ValueFactory.newInteger(val));
		return this;
	}
	public MapBuilder2 put(String key,float val) {
		put(key,ValueFactory.newFloat(val));
		return this;
	}
	public MapBuilder2 put(String key,double val) {
		put(key,ValueFactory.newFloat(val));
		return this;
	}
	public MapBuilder2 put(String key,byte[] val) {
		put(key,ValueFactory.newBinary(val));
		return this;
	}
	public MapBuilder2 put(String key,boolean val) {
		put(key,ValueFactory.newBoolean(val));
		return this;
	}
	public ValueFactory.MapBuilder wrap() {
		return wrap;
	}
	public MapValue build() {
		return wrap.build();
	}
}
