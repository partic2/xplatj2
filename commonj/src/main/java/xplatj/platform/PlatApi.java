package xplatj.platform;

import java.io.File;

//instance at xplatj.gdxconfig.core.PlatCoreConfig.platApi
public interface PlatApi {
	ClassLoader load(File[] cp, ClassLoader parent);
	File cacheDir();
	public String[] getStoragePathList();
}
