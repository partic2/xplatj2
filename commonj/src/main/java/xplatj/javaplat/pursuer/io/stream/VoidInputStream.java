package xplatj.javaplat.pursuer.io.stream;

import java.io.*;

public class VoidInputStream extends InputStream {

	@Override
	public int read() throws IOException {
		try {
			Thread.sleep(1000 * 60 * 60 * 24);
		} catch (InterruptedException e) {
		}
		return 0;
	}

}
