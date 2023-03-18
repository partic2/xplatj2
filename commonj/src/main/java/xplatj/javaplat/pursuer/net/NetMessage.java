package xplatj.javaplat.pursuer.net;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class NetMessage {
	private NetAddress from;
	private ByteBuffer buf;

	public NetMessage() {
	}
	
	public NetMessage(byte[] data) {
		setData(data);
	}
	
	public void setSource(NetAddress addr) {
		from = addr;
	}

	public NetAddress getSource() {
		return from;
	}
	
	public ByteBuffer getBuffer(){
		return buf;
	}
	
	public void setBuffer(ByteBuffer buf){
		this.buf=buf;
	}
	public void setData(byte[] data){
		buf=ByteBuffer.wrap(data);
	}
	
	public byte[] getData(){
		((Buffer)buf).mark();
		byte[] r=new byte[buf.remaining()];
		buf.get(r);
		((Buffer)buf).reset();
		return r;
	}

}
