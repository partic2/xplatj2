package xplatj.javaplat.pursuer.net;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class NetStringMessage extends NetMessage{
	
	static byte[] magic = new byte[] { '1', '0', '3', '2' };

	public NetStringMessage() {
	}

	public NetStringMessage(String m) {
		setMessage(m);
	}

	public NetStringMessage(NetMessage m){
		setSource(m.getSource());
		setData(m.getData());
	}

	public void setMessage(String m) {
		byte[] b;
		try {
			b = m.getBytes("UTF-8");
			byte[] ret = new byte[magic.length + b.length];
			System.arraycopy(magic, 0, ret, 0, magic.length);
			System.arraycopy(b, 0, ret, magic.length, b.length);
			setData(ret);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public String getMessage() {
		byte[] buff = getData();
		
		byte[] head = Arrays.copyOf(buff, magic.length);
		String msg;
		if (!Arrays.equals(head, magic)) {
			return null;
		}
		byte[] b = Arrays.copyOfRange(buff, magic.length, buff.length);
		try {
			msg = new String(b, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
		return msg;
	}
}

