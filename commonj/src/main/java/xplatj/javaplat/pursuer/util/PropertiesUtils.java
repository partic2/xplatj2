package xplatj.javaplat.pursuer.util;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;


public class PropertiesUtils {
	public Properties properties;
	public PropertiesUtils() {
	}
	public PropertiesUtils(Properties prop){
		properties=prop;
	}
	public Iterable<Map.Entry<String, String>> startWith(String prefix,boolean eliminatePrefix){
		LinkedList<Map.Entry<String, String>> list=new LinkedList<Map.Entry<String,String>>();
		for(Entry<Object, Object> ent:properties.entrySet()){
			KVPair<String, String> pair=new KVPair<String,String>((String)ent.getKey(),(String)ent.getValue());
			if(pair.key.startsWith(prefix)){
				if(eliminatePrefix){
					pair.key=pair.key.substring(prefix.length());
				}
				list.push(pair);
			}
		}
		return list;
	}
	public Map<String,String> toMap(){
		return (Map<String, String>) this;
	}
	
}
