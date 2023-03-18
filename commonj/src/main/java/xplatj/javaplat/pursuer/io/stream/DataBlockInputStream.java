package xplatj.javaplat.pursuer.io.stream;

import java.io.*;

import xplatj.javaplat.pursuer.io.IDataBlock;

public class DataBlockInputStream extends InputStream {
	protected IDataBlock db;
	protected long offset;

	public DataBlockInputStream(IDataBlock d) {
		db = d;
		offset = 0;
	}

	public IDataBlock getDataBlock() {
		return db;
	}

	@Override
	public int available() throws IOException {
		return (int) (db.size() - offset);
	}

	@Override
	public int read() throws IOException {
		byte[] ret = new byte[1];
		int r = read(ret);
		if (r < 1) {
			return -1;
		} else {
			return ret[0];
		}
	}

	@Override
	public int read(byte[] buffer, int byteOffset, int byteCount)
			throws IOException {
		try {
			db.lock().lockInterruptibly();
		} catch (InterruptedException e) {
			throw new InterruptedIOException();
		}
		if (db.pos() != offset) {
			db.seek(offset);
		}
		int ret = db.read(buffer, byteOffset, byteCount);
		offset = db.pos();
		db.lock().unlock();
		if (ret==0) {
			return -1;
		}
		return ret;

	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset){
		this.offset=offset;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	long recOff;

	@Override
	public void mark(int readlimit) {
		recOff = offset;
	}

	@Override
	public void reset() throws IOException {
		offset = recOff;
	}

	public void rewind() {
		offset = 0;
	}
}
