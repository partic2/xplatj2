package xplatj.javaplat.partic2.net;

import java.net.DatagramSocket;
import java.net.SocketException;

import xplatj.javaplat.partic2.util.IFactory;

public class DefaultDatagramSocketFactory implements IFactory<DatagramSocket> {

	@Override
	public DatagramSocket create() {
		try {
			return new DatagramSocket(null);
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}

}
