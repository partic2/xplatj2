package xplatj.javaplat.partic2.net;

import java.io.IOException;
import java.net.ServerSocket;

import xplatj.javaplat.partic2.util.IFactory;

public class DefaultServerSocketFactory implements IFactory<ServerSocket> {

	@Override
	public ServerSocket create() {
		try {
			return new ServerSocket();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
