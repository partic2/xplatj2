package xplatj.javaplat.pursuer.net;

import java.net.Socket;

import xplatj.javaplat.pursuer.util.IFactory;

public class DefaultSocketFactory implements IFactory<Socket> {

	@Override
	public Socket create() {
		return new Socket();
	}

}
