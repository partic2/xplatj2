package xplatj.javaplat.pursuer.util;

public interface Parcelable {
	byte[] saveToBytes();

	boolean loadFromBytes(byte[] buff);
}
