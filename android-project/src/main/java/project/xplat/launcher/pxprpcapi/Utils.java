package project.xplat.launcher.pxprpcapi;

import pursuer.patchedmsgpack.core.MessageBufferPacker;
import pursuer.patchedmsgpack.core.MessagePack;
import pursuer.patchedmsgpack.value.Value;
import pursuer.patchedmsgpack.value.ValueFactory;
import xplatj.gdxconfig.core.PlatCoreConfig;

import java.io.IOException;

public class Utils {
    public static String joinStringList(Iterable<String> s,String delim){
        StringBuilder sb=new StringBuilder();
        for(String e : s){
            sb.append(e);
            sb.append(delim);
        }
        return sb.toString();
    }
    public String repr(Object o){
        return o.toString();
    }
    //slow, avoid to use.
    public static Value msgpackValueFrom(Object o) {
    	if(o==null) {
    		return ValueFactory.newNil();
    	}else {
    		Class<? extends Object> c = o.getClass();
    		if(c==String.class) {
    			return ValueFactory.newString((String)o);
    		}else if(c==Integer.class) {
    			return ValueFactory.newInteger((Integer)o);
    		}else if(c==Long.class) {
    			return ValueFactory.newInteger((Long)o);
    		}else if(c==Double.class) {
    			return ValueFactory.newFloat((Double)o);
    		}else if(c==Float.class) {
    			return ValueFactory.newFloat((Float)o);
    		}else {
    			return ValueFactory.newString(o.toString());
    		}
    	}
    }
    public static byte[] packFrom(Value v) {
    	MessageBufferPacker mp=MessagePack.newDefaultBufferPacker();
    	try {
			mp.packValue(v);
		} catch (IOException e) {
			throw new RuntimeException("msgpack error:"+e.toString());
		}
    	return mp.toByteArray();
    }
}
