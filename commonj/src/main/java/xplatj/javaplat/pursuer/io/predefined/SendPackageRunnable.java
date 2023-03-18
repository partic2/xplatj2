package xplatj.javaplat.pursuer.io.predefined;

import java.io.*;

import xplatj.javaplat.pursuer.io.stream.PackageIOStream;

public class SendPackageRunnable implements Runnable {
	public PackageIOStream target;
	public byte[] content;
	public IOException error;

	public SendPackageRunnable(PackageIOStream target, byte[] content) {
		this.target = target;
		this.content = content;
		error = null;
	}

	public void run() {
		try {
			target.sendPackage(content);
		} catch (IOException e) {
			error = e;
		}
	}
}
