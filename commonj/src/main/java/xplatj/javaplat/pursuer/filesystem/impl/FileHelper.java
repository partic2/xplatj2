package xplatj.javaplat.pursuer.filesystem.impl;

import java.io.File;
import java.util.*;

public class FileHelper {
	public boolean rmf(File f) {
		File[] subf;
		int i;
		if (f.canWrite() == false) {
			return false;
		}
		if (f.isDirectory()) {
			subf = f.listFiles();
			for (i = 0; i < subf.length; i++) {
				rmf(subf[i]);
			}
			return f.delete();
		} else {
			return f.delete();
		}
	}

	public boolean rmfs(Iterable<File> fs) {
		boolean re = true;
		Iterator<File> i = fs.iterator();
		while (!i.hasNext()) {
			re &= rmf(i.next());
		}
		return re;
	}

	public Iterable<File> findInDir(File dir, String pattern) {
		Stack<File> rf = new Stack<File>();
		File subfs[];
		int i;
		if (dir.isDirectory()) {
			subfs = dir.listFiles();
			for (i = 0; i < subfs.length; i++) {
				if (subfs[i].getName().matches(pattern)) {
					rf.push(subfs[i]);
				}
			}
		}
		return rf;
	}
}
