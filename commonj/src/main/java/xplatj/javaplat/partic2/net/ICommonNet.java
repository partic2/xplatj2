package xplatj.javaplat.partic2.net;

import java.io.Closeable;
import java.io.IOException;

import xplatj.javaplat.partic2.util.Container;
import xplatj.javaplat.partic2.util.EventHandler;

public interface ICommonNet extends Closeable{
	int getMessageBufferSize();
	
	void setMessageBufferSize(int size);
	
	void sendMessage(NetMessage packet, NetAddress addr);

	NetConnection connectTo(NetAddress addr) throws IOException;

	Container<EventHandler<ICommonNet, NetMessage>> getOnMsgRecv();
	
	Container<EventHandler<ICommonNet, NetConnection>> getOnConnect();
	
	void updateNetStatus() throws IOException;
	
}
