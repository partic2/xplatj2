package xplatj.javaplat.pursuer.net;

import java.net.InetSocketAddress;

public class DefaultCommonNetAddress implements NetAddress {
	protected InetSocketAddress socketAddr;
	public static final String protocol="xplat-dcna1";
	public DefaultCommonNetAddress(InetSocketAddress socketAddr) {
		this.socketAddr=socketAddr;
	}
	protected DefaultCommonNetAddress() {};
	@Override
	public String getProtocol() {
		return protocol;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof DefaultCommonNetAddress) {
			return ((DefaultCommonNetAddress) obj).getSocketAddress().equals(socketAddr);
		}else {
			return false;
		}
	}
	@Override
	public int hashCode() {
		return socketAddr.hashCode();
	}
	public InetSocketAddress getSocketAddress() {
		return socketAddr;
	}
}
