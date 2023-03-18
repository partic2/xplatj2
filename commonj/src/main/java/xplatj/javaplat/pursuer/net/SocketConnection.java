package xplatj.javaplat.pursuer.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketConnection implements NetConnection{
	public SocketConnection() {
	}

	public boolean isServ;
	public Socket sock;

	public void useSocket(Socket sock, boolean isServ) {
		this.sock = sock;
		this.isServ = isServ;
	}

	public boolean isServ() {
		return isServ;
	}
	
	public Socket getSocket(){
		return sock;
	}

	public void setTimeout(int ms) throws IOException {
		sock.setSoTimeout(ms);
	}

	public InputStream read() throws IOException {
		return sock.getInputStream();
	}

	public OutputStream write() throws IOException {
		return sock.getOutputStream();
	}

	public boolean isOpen() {
		return !sock.isClosed();
	}

	public void close() throws IOException {
		sock.close();
	}

	public NetAddress connectedAddress;
	public NetAddress getConnectedAddress() {
		if(connectedAddress==null) {
			connectedAddress=new DefaultCommonNetAddress(new InetSocketAddress(sock.getInetAddress(),sock.getPort()));
		}
		return connectedAddress;
	}
}
