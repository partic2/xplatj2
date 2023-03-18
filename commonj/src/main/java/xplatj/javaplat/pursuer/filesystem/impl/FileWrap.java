package xplatj.javaplat.pursuer.filesystem.impl;

import java.io.*;
import java.util.*;
import xplatj.javaplat.pursuer.filesystem.*;
import xplatj.javaplat.pursuer.io.*;
import xplatj.javaplat.pursuer.io.datablock.*;

public class FileWrap implements IFile {

	private File hf;
	private RandomAccessFile raf;
	private FileDataBlock fdb;
	private int refCount;
	private String fsPath;
	private IFileSystem fs;

	@Override
	public String getPath() {
		return fsPath;
	}

	@Override
	public IDataBlock open() throws IOException {
		if (refCount <= 0) {
			if (!hf.isFile()) {
				throw new FileNotFoundException("File not exist.");
			}
			raf = new RandomAccessFile(hf, "rw");
			fdb = new FileDataBlockWrap(this, raf);
		}
		refCount++;
		return fdb;
	}

	public int close() throws IOException {
		refCount--;
		if (raf != null && refCount <= 0) {
			raf.close();
			raf = null;
			fdb = null;
		}
		return refCount;
	}

	public FileWrap(String path, IFileSystem fs, String fsPath) {
		hf = new File(path);
		this.fsPath = fsPath;
		this.fs = fs;
		refCount = 0;
	}

	@Override
	public IFileSystem getFileSystem() {
		return fs;
	}

	@Override
	public File getJavaFile() {
		return hf;
	}

	@Override
	public IFile next(String next){
		String npath = getPath() + "/" + next;
		return getFileSystem().resolve(npath);
	}

	@Override
	public IFile last(){
		String npath = getPath();
		npath = npath.substring(0, npath.lastIndexOf("/"));
		return getFileSystem().resolve(npath);
	}

	@Override
	public Iterable<String> list(){
		String[] s = hf.list();
		if (s == null) {
			return null;
		} else {
			return Arrays.asList(s);
		}
	}

	@Override
	public void create() throws IOException{
		hf.getParentFile().mkdirs();
		if(!hf.createNewFile()){
			IDataBlock db=open();
			db.resize(0);
			db.free();
		}
	}

	@Override
	public void delete() throws IOException{
		if(!hf.delete()){
			throw new IOException("delete failed");
		}
	}

	@Override
	public boolean exists(){
		return hf.exists();
	}

	public boolean canOpen() {
		return hf.isFile();
	}
	@Override
	public void rename(String newName) throws IOException{
		File newFile = getFileSystem().resolve(newName).getJavaFile();
		newFile.getParentFile().mkdirs();
		if(!hf.renameTo(newFile)){
			throw new IOException("rename failed");
		}
	}
}
