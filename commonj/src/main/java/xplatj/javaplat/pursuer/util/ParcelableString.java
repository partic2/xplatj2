package xplatj.javaplat.pursuer.util;

import java.io.UnsupportedEncodingException;

public class ParcelableString implements Parcelable {
	String msg;

	public ParcelableString() {
	}

	public ParcelableString(String s) {
		setString(s);
	}

	public String getString() {
		return msg;
	}

	public void setString(String s) {
		msg = s;
	}

	@Override
	public byte[] saveToBytes() {
		try {
			return msg.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	@Override
	public boolean loadFromBytes(byte[] buff) {
		try {
			msg = new String(buff, "UTF-8");
			return true;
		} catch (UnsupportedEncodingException e) {
			return false;
		}
	}

}
