package xplatj.javaplat.pursuer.io.datablock;

import java.io.*;
import java.nio.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import xplatj.javaplat.pursuer.io.*;

public class ByteBufferDataBlock implements IDataBlock {
	protected ByteBuffer buff;
	private Lock mtx;

	public ByteBufferDataBlock() {
	}

	public void init(ByteBuffer b) {
		setByteBuffer(b);
		mtx = new ReentrantLock();
	}

	public void setByteBuffer(ByteBuffer b) {
		buff = b;
	}

	@Override
	public void seek(long pos) throws IOException {
		buff.position((int) pos);
	}

	@Override
	public long pos() throws IOException {
		return buff.position();
	}

	@Override
	public long size() throws IOException {
		return buff.limit();
	}

	@Override
	public void resize(long size) throws IOException {
		throw new IOException("this object not support resize");
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		int available=(int) (size() - pos());
		len = Math.min(available, len);
		buff.get(buffer, off, len);
		return len;
	}

	@Override
	public int write(byte[] buffer, int off, int len) throws IOException {
		len = Math.min((int) (size() - pos()), len);
		buff.put(buffer, off, len);
		return len;
	}

	@Override
	public void free() {
		buff = null;
	}

	@Override
	public Lock lock() {
		return mtx;
	}


}
