package xplatj.javaplat.pursuer.io;

import java.io.*;
import java.util.concurrent.locks.Lock;

import xplatj.javaplat.pursuer.util.Container;
import xplatj.javaplat.pursuer.util.EventHandler;


public interface IDataBlock {
	
	void seek(long pos) throws IOException;

	long pos() throws IOException;

	long size() throws IOException;

	void resize(long size) throws IOException;

	int read(byte[] buffer, int off, int len) throws IOException;

	int write(byte[] buffer, int off, int len) throws IOException;

	Lock lock();

	void free();

}
