package xplatj.platform;

import java.io.File;

//instance at xplatj.javaplat.partic2.util.PlatCoreConfig.platApi
public interface PlatApi {
	ClassLoader load(File[] cp, ClassLoader parent);
	File cacheDir();
	public String[] getStoragePathList();
}
