package xplatj.javaplat.pursuer.filesystem.impl;

import java.util.*;
import java.util.Map.Entry;
import java.io.*;

import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.pursuer.filesystem.*;
import xplatj.javaplat.pursuer.io.IDataBlock;
import xplatj.javaplat.pursuer.util.PropertiesUtils;

public class PrefixFS implements IFileSystem {

	protected ArrayList<String> org;
	protected ArrayList<String> replace;
	boolean inited;

	public static String separator = "/";

	public PrefixFS() {
		org = new ArrayList<String>(32);
		replace = new ArrayList<String>(32);
		inited = false;

	}

	/*
	 *  OS can be null Android or Windows, Linux,if iniFile==null,iniFile will be located depend on os
	 */
	public synchronized boolean init(String os, InputStream iniFile) {
		Properties inicfg;
		if (iniFile == null) {
			try {
				if (os.equals("Android")) {
					iniFile = new FileInputStream("/sdcard/xplat/cfg.ini");
				} else if (os.equals("Windows")||os.equals("Linux")) {
					iniFile = new FileInputStream("res/cfg.ini");
				} else {
					return false;
				}
			} catch (FileNotFoundException e) {
				return false;
			}
		}
		inicfg = new Properties();
		try {
			inicfg.load(iniFile);
			Iterable<Entry<String, String>> fp = new PropertiesUtils(inicfg).startWith("filesystem.prefix.",true);
			iniFile.close();
			if(PlatCoreConfig.platApi!=null) {
				for(String e:PlatCoreConfig.platApi.getStoragePathList()) {
					org.add("/1/"+e);
					replace.add(e);
				}
			}
			for(Entry<String, String> t:fp) {
				org.add(t.getKey());
				replace.add(t.getValue());
			}
			inited = true;
			return true;
		} catch (IOException e) {
			return false;
		}

	}

	public String convert(String path) {
		String result="";
		for (int i = 0; i < org.size(); i++) {
			if (prefixmatch(org.get(i), path)) {
				result = replace.get(i).concat(path.substring(org.get(i).length()));
				break;
			}
		}
		return result;
	}

	protected class NoMatchPathDir implements IFile{

		@Override
		public IDataBlock open() throws IOException {
			throw new IOException("Not suppoert open()");
		}

		@Override
		public File getJavaFile() {
			return null;
		}

		@Override
		public IFileSystem getFileSystem() {
			return PrefixFS.this;
		}

		@Override
		public String getPath() {
			return "";
		}

		@Override
		public IFile next(String next) {
			return getFileSystem().resolve(next);
		}

		@Override
		public IFile last() {
			return null;
		}

		@Override
		public Iterable<String> list() {
			return PrefixFS.this.org;
		}

		@Override
		public void create() throws IOException {
			throw new IOException("Not suppoert create()");
		}

		@Override
		public void delete() throws IOException {
			throw new IOException("Not suppoert delete()");
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public boolean canOpen() {
			return false;
		}

		@Override
		public void rename(String newName) throws IOException {
			throw new IOException("Not suppoert rename()");
		}
		
	}
	@Override
	public IFile resolve(String path) {
		String convertPath = convert(path);
		if(convertPath.length()==0) {
			return new NoMatchPathDir();
		}else {
			FileWrap fw = new FileWrap(convertPath, this, path);
			return fw;
		}
	}

	protected boolean prefixmatch(String ptn, String match) {
		int len = ptn.length();
		if (match.length() < len) {
			return false;
		}
		return ptn.equals(match.substring(0, len));
	}

}
