package xplatj.javaplat.partic2.filesystem.impl;

import xplatj.javaplat.partic2.filesystem.*;

public class PrefixFS implements IFileSystem {
    public static String defaultPrefix="";

    public String prefix;
	public static String separator = "/";

	public PrefixFS() {
		this.prefix=PrefixFS.defaultPrefix;
	}


	public String convert(String path) {
		return prefix+path;
	}

	@Override
	public IFile resolve(String path) {
        FileWrap fw = new FileWrap(this.convert(path), this, path);
        return fw;
	}

}
