package xplatj.javaplat.pursuer.io.stream;

import java.io.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import xplatj.javaplat.pursuer.util.EventHandler;

public class StreamTransmit implements Runnable {
	private Executor exec;
	private InputStream in;
	private OutputStream out;
	private boolean finished;
	private boolean terminated;
	private byte[] buff;
	private int byteRemain;
	private EventHandler<StreamTransmit, Integer> int_complete;

	public StreamTransmit(Executor thread, InputStream from, OutputStream to, int byteToTrans, int bufferSize,
			EventHandler<StreamTransmit, Integer> onComplete) {
		start(thread, from, to, byteToTrans, bufferSize, onComplete);

	}

	public StreamTransmit() {
	}

	/**
	 * 
	 * @param thread
	 *            pool or null for block call
	 * @param from
	 * @param to
	 * @param byte
	 *            to transform
	 * @param buffer
	 *            size
	 * @param run
	 *            on complete
	 */
	public void start(Executor thread, InputStream from, OutputStream to, int byteToTrans, int bufferSize,
			EventHandler<StreamTransmit, Integer> onComplete) {
		if (bufferSize == 0) {
			bufferSize = 0x100;
		}
		exec = thread;
		finished = false;
		terminated = false;
		in = from;
		out = to;
		byteRemain = byteToTrans;
		buff = new byte[bufferSize];
		int_complete = onComplete;

		if (exec == null) {
			run();
		} else {
			exec.execute(this);
		}
	}

	public boolean finished() {
		return finished;
	}

	public void terminate() {
		terminated = true;
	}

	public int getByteRemained() {
		return byteRemain;
	}

	@Override
	public void run() {
		try {
			int read;
			for (read = in.read(buff, 0, Math.min(buff.length, byteRemain)); read != -1
					&& byteRemain > 0; read = in.read(buff, 0, Math.min(buff.length, byteRemain))) {
				out.write(buff, 0, read);
				byteRemain -= read;
				if (terminated) {
					break;
				}
			}
		} catch (IOException e) {
		}
		finished = true;
		if (int_complete != null) {
			int_complete.handle(this, 0);
		}
	}
}
