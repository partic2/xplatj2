package xplatj.javaplat.pursuer.io.stream;

import java.io.IOException;
import java.io.OutputStream;

import xplatj.javaplat.pursuer.util.EventHandler;

public class NotifyBytesOutputStream extends OutputStream {
	private EventHandler<NotifyBytesOutputStream, Integer> onRequire;
	byte[] buf;
	int pos;
	int count;

	public NotifyBytesOutputStream() {
	}

	public int available() throws IOException {
		return count - pos;
	}

	public void supply(byte[] data) {
		buf = data;
		pos = 0;
		count = data.length;
	}

	public byte[] getBuf() {
		return buf;
	}

	protected void onRequireData() {
		if (onRequire != null) {
			onRequire.handle(this, 1);
		}
	}

	public void setOnRequireData(EventHandler<NotifyBytesOutputStream, Integer> callback) {
		onRequire = callback;
	}

	@Override
	public void write(int b) throws IOException {
		if (available() > 0) {
			buf[pos] = (byte) b;
			pos++;
		} else {
			onRequireData();
			if (available() > 0) {
				buf[pos] = (byte) b;
				pos++;
			} else {
				throw new IOException("No available data.");
			}
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		int rest = len;
		int nwrite = 0;
		int brest = available();
		if (buf == null) {
			onRequireData();
		}
		while (rest > brest) {
			System.arraycopy(b, pos, buf, off + nwrite, brest);
			rest -= brest;
			nwrite += brest;
			onRequireData();
			brest = available();
			if (brest == 0) {
				return;
			}
		}
		System.arraycopy(buf, pos, b, off + nwrite, rest);
		nwrite += rest;
		return;
	}
}
