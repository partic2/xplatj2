package xplatj.javaplat.partic2.util;

public interface Parcelable {
	byte[] saveToBytes();

	boolean loadFromBytes(byte[] buff);
}
