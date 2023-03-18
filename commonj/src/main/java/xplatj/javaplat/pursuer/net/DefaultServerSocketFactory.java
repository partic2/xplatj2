package xplatj.javaplat.pursuer.net;

import java.io.IOException;
import java.net.ServerSocket;

import xplatj.javaplat.pursuer.util.IFactory;

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
