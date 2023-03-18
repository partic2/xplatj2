package xplatj.javaplat.pursuer.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;

public interface NetConnection extends Channel {

	public boolean isServ();

	public void setTimeout(int ms) throws IOException;

	public InputStream read() throws IOException;

	public OutputStream write() throws IOException;

	public NetAddress getConnectedAddress();
}
