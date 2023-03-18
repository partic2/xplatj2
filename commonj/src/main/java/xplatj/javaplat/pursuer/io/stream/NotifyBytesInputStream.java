package xplatj.javaplat.pursuer.io.stream;

import java.io.IOException;
import java.io.InputStream;

import xplatj.javaplat.pursuer.util.EventHandler;

public class NotifyBytesInputStream extends InputStream {
	private EventHandler<NotifyBytesInputStream, Integer> onRequire;
	protected byte[] buf;
	protected int pos;
	protected int count;

	public NotifyBytesInputStream() {

	}

	@Override
	public int available() throws IOException {
		return count - pos;
	}

	public void supply(byte[] data, int num) {
		buf = data;
		pos = 0;
		count = Math.min(data.length, num);
	}

	public byte[] getBuf() {
		return buf;
	}

	protected void onRequireData() {
		if (onRequire != null) {
			onRequire.handle(this, 1);
		}
	}

	public void setOnRequireData(EventHandler<NotifyBytesInputStream, Integer> callback) {
		onRequire = callback;
	}

	@Override
	public int read() throws IOException {
		int ret;
		if (available() > 0) {
			ret = 0xff & buf[pos];
			pos++;
		} else {
			onRequireData();
			if (available() > 0) {
				ret = 0xff & buf[pos];
				pos++;
			} else {
				return -1;
			}
		}
		return ret;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (buf == null && len > 0) {
			onRequireData();
		}
		int rest = len;
		int nread = 0;
		int brest = available();
		while (rest > brest) {
			System.arraycopy(buf, pos, b, off + nread, brest);
			rest -= brest;
			nread += brest;
			onRequireData();
			brest = available();
			if (brest == 0) {
				if (nread == 0) {
					nread = -1;
				}
				return nread;
			}
		}
		System.arraycopy(buf, pos, b, off + nread, rest);
		pos += rest;
		nread += rest;
		return nread;
	}

}
