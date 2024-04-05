package xplatj.javaplat.partic2.net;

import java.net.Socket;

import xplatj.javaplat.partic2.util.IFactory;

public class DefaultSocketFactory implements IFactory<Socket> {

	@Override
	public Socket create() {
		return new Socket();
	}

}
