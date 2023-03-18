package project.xplatj.backend.jse;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import xplatj.platform.PlatApi;

public class PlatApiImpl implements PlatApi {

	public ClassLoader load(File[] cp, ClassLoader parent) {
		URL[] us = new URL[cp.length];
		for (int i = 0; i < cp.length; i++) {
			try {
				us[i] = cp[i].toURI().toURL();
			} catch (MalformedURLException e) {
				return null;
			}
		}
		URLClassLoader loader = new URLClassLoader(us, parent);
		return loader;
	}

	@Override
	public File cacheDir() {
		return new File(System.getProperty("java.io.tmpdir"));
	}

	@Override
	public String[] getStoragePathList() {
		File[] roots = File.listRoots();
		String[] list=new String[roots.length];
		for(int i=0;i<roots.length;i++){
			list[i]=roots[i].getAbsolutePath();
		}
		return list;
	}
}
