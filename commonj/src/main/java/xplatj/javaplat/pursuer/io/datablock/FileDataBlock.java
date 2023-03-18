package xplatj.javaplat.pursuer.io.datablock;

import java.io.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import xplatj.javaplat.pursuer.io.IDataBlock;

public class FileDataBlock implements IDataBlock {

	RandomAccessFile f;
	long markPos;
	Lock mtx;

	public FileDataBlock() {
		mtx = new ReentrantLock();
	}

	public void init(RandomAccessFile file) throws IOException {
		f = file;
		f.seek(0);
	}

	public RandomAccessFile getFile() {
		return f;
	}

	@Override
	public void seek(long pos) throws IOException {
		f.seek(pos);
	}

	@Override
	public long pos() throws IOException {
		return f.getFilePointer();
	}

	@Override
	public long size() throws IOException{
		return f.length();
	}

	@Override
	public void resize(long size) throws IOException {
		f.setLength(size);
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		int ret = Math.max(0, f.read(buffer, off, len));
		return ret;
	}
	@Override
	public int write(byte[] buffer, int off, int len) throws IOException {
		f.write(buffer, off, len);
		return len;
	}

	@Override
	public void free() {
		try {
			f.close();
		} catch (IOException e) {
		}
	}

	@Override
	public Lock lock() {
		return mtx;
	}

}
