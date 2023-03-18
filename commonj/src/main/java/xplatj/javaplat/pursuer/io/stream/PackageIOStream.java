package xplatj.javaplat.pursuer.io.stream;

import java.io.*;
import java.net.*;
import java.io.DataInputStream;

public class PackageIOStream {
	DataInputStream IOInput;
	DataOutputStream IOOutput;
	byte recvData[];
	int lastBytes;

	public PackageIOStream() {
	}

	public PackageIOStream(InputStream in, OutputStream out) {
		configure(in, out);
	}

	public boolean configure(InputStream in, OutputStream out) {
		IOInput = new DataInputStream(in);
		IOOutput = new DataOutputStream(out);
		return true;
	}

	public boolean configure(Socket s) {
		try {
			return configure(s.getInputStream(), s.getOutputStream());
		} catch (IOException e) {
			return false;
		}
	}

	public byte[] waitPackage() throws IOException {
		lastBytes = IOInput.readInt();
		if (lastBytes >> 12 != 0 || lastBytes < 0) {
			throw (new IOException());
		}
		recvData = new byte[lastBytes];
		IOInput.readFully(recvData);
		return recvData;
	}

	public synchronized void sendPackage(byte[] sendData) throws IOException {
		IOOutput.writeInt(sendData.length);
		IOOutput.write(sendData);
		IOOutput.flush();
	}
}
