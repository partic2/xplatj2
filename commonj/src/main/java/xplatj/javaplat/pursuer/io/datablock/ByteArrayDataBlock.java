package xplatj.javaplat.pursuer.io.datablock;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import xplatj.javaplat.pursuer.io.IDataBlock;

public class ByteArrayDataBlock implements IDataBlock {
	private byte[] buf;
	private int pos;
	private Lock mtx;

	public ByteArrayDataBlock() {
	}
	
	public ByteArrayDataBlock(byte[] arr) {
		init(arr);
	}
	
	public boolean init(byte[] arr) {
		buf = arr;
		mtx = new ReentrantLock();
		return true;
	}

	@Override
	public void seek(long pos) throws IOException {
		this.pos = (int) pos;
	}

	@Override
	public long pos() throws IOException {
		return pos;
	}

	@Override
	public long size() throws IOException {
		return buf.length;
	}

	@Override
	public void resize(long size) throws IOException {
		buf = Arrays.copyOf(buf, (int) size);
	}

	protected int available() throws IOException {
			return (int) (size() - pos);
	}

	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		int available=available();
		int tr;
		tr = Math.min(available, len);
		System.arraycopy(buf, pos, buffer, off, tr);
		pos+=tr;
		return tr;
	}

	@Override
	public int write(byte[] buffer, int off, int len) throws IOException {
		int tr;
		tr = Math.min(available(), len);
		System.arraycopy(buffer, pos, buf, off, tr);
		pos+=tr;
		return tr;
	}

	@Override
	public void free() {
		buf = null;
	}

	@Override
	public Lock lock() {
		return mtx;
	}

}
