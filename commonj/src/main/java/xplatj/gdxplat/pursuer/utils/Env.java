package xplatj.gdxplat.pursuer.utils;

import xplatj.gdxconfig.core.PlatCoreConfig;

public class Env {
	public static <T> T i(Class<T> type) {
		return instantiate(type);
	}
	public static <T> T instantiate(Class<T> type) {
		T ins=Env.s(type);
		if(ins==null) {
			return PlatCoreConfig.get().context.getSoftSingleton(type);
		}else {
			return ins;
		}
		
	}
	public static <T> T t(Class<T> type){
		return temp(type);
	}
	public static <T> T s(Class<T> type){
		return singleton(type);
	}
	public static <T> T singleton(Class<T> type){
		return PlatCoreConfig.get().context.getHardSingleton(type);
	}
	public static <T> void ss(Class<T> type,T val){
		setSingleton(type, val);
	} 
	public static <T> void setSingleton(Class<T> type,T val){
		PlatCoreConfig.get().context.setHardSingleton(type, val);
	}
	public static <T> T temp(Class<T> type){
		return PlatCoreConfig.get().context.getWeakSingleton(type);
	}
	public static void remove(Class<?> type){
		PlatCoreConfig.get().context.removeSoftSingleton(type);
	}
	public static void link(Class<?> replacedClass,Class<?> newClass){
		PlatCoreConfig.get().context.linkSoftSingleton(replacedClass, newClass);
	}
}

