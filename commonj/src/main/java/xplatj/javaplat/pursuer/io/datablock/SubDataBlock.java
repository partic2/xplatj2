package xplatj.javaplat.pursuer.io.datablock;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import xplatj.javaplat.pursuer.io.IDataBlock;

public class SubDataBlock implements IDataBlock {

	IDataBlock data;
	long base;
	long end;

	public boolean init(IDataBlock d, long base, long end) {
		try {
			if (d.size() < end) {
				return false;
			}
		} catch (IOException e) {
			return false;
		}
		data = d;
		this.base = base;
		this.end = end;
		return true;
	}

	@Override
	public void seek(long pos) throws IOException {
		if (pos + base > end) {
			IOException e = new IOException();
			throw (e);
		}
		data.seek(pos + base);
	}

	@Override
	public long pos() throws IOException {
		return data.pos() - base;
	}

	@Override
	public long size() throws IOException {
		return end - base;
	}

	@Override
	public void resize(long size) throws IOException {
		end = base + size;
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		int rest = (int) (end - pos());
		return Math.min(data.read(buffer, off, len), rest);
	}

	@Override
	public int write(byte[] buffer, int off, int len) throws IOException {
		int rest = (int) (end - pos());
		if (rest == 0) {
			return -1;
		} else if (rest < buffer.length) {
			throw new IOException("Out of range.");
		}
		return data.write(buffer, off, len);
	}

	@Override
	public void free() {
	}

	@Override
	public Lock lock() {
		return data.lock();
	}


}
