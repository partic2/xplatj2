package xplatj.javaplat.pursuer.io.stream;

import java.io.*;

import xplatj.javaplat.pursuer.io.IDataBlock;

public class DataBlockOutputStream extends OutputStream {
	protected IDataBlock db;
	protected long offset;

	public DataBlockOutputStream(IDataBlock d) {
		db = d;
		offset = 0;
	}

	public IDataBlock getDataBlock() {
		return db;
	}

	@Override
	public void write(int p1) throws IOException {

		byte[] bs = new byte[1];
		bs[0] = (byte) p1;
		write(bs);
	}

	@Override
	public void write(byte[] buffer, int offset, int count) throws IOException {
		try {
			db.lock().lockInterruptibly();
		} catch (InterruptedException e) {
			throw new InterruptedIOException();
		}
		if(db.pos()!= this.offset){
			db.seek(this.offset);
		}
		db.write(buffer, offset, count);
		this.offset =db.pos();
		db.lock().unlock();
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset){
		offset =offset;
	}

	public void seekToEnd(){
		try {
			offset =db.size();
		} catch (IOException e) {
			offset =0;
		}
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

	public void rewind() {
		offset = 0;
	}
}
