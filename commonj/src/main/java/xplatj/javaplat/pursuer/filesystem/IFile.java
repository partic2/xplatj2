package xplatj.javaplat.pursuer.filesystem;

import java.io.*;

import xplatj.javaplat.pursuer.filesystem.impl.FileWrap;
import xplatj.javaplat.pursuer.io.IDataBlock;

public interface IFile{
	IDataBlock open() throws IOException;

	File getJavaFile();

	IFileSystem getFileSystem();

	String getPath();
	
	IFile next(String next);

	IFile last();

	Iterable<String> list();

	void create() throws IOException;

	void delete() throws IOException;

	boolean exists();
	
	boolean canOpen();

	void rename(String newName) throws IOException;

}
