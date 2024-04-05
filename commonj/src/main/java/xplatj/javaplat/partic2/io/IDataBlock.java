package xplatj.javaplat.partic2.io;

import java.io.*;
import java.util.concurrent.locks.Lock;


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
