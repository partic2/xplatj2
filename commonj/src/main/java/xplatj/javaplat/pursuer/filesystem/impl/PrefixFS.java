package xplatj.javaplat.pursuer.filesystem.impl;

import java.util.*;
import java.util.Map.Entry;
import java.io.*;

import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.pursuer.filesystem.*;
import xplatj.javaplat.pursuer.io.IDataBlock;
import xplatj.javaplat.pursuer.util.PropertiesUtils;

public class PrefixFS implements IFileSystem {
    public static String defaultPrefix="";

    protected String prefix;
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
