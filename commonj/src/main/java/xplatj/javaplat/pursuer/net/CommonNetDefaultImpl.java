package xplatj.javaplat.pursuer.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Executor;

import xplatj.javaplat.pursuer.util.Container;
import xplatj.javaplat.pursuer.util.EventHandler;

public class CommonNetDefaultImpl implements ICommonNet {
	private Executor thread;
	private Collection<DatagramChannel> dsocsServer;
	private Collection<DatagramChannel> dsocsClient;
	private ServerSocketChannel anyssoc;
	private boolean stopped;
	private Config cfg;
	private Selector selector;
	private SelectThread selectorT;
	
	public static int protocolVersion=1;
	public static final byte CommandPacket=1;
	public static final byte DataPacket=2;
	public static final byte CommandResponsePacket=3;

	/*
	 * If msgPort<0,No UDP port will be occupied for listening. If streamPort<0,No
	 * TCP port will be occupied for listening.
	 */
	public static class Config {
		public int msgPort = 2052;
		public boolean ipv4 = true;
		public boolean ipv6 = false;
		public boolean filterSelf = true;
		public int messageBufferSize = 0x1000;
	}

	public CommonNetDefaultImpl(Executor exec) {
		cfg = new Config();
		pinit(exec);
	}

	public CommonNetDefaultImpl(Executor exec, Config netCfg) {
		cfg = netCfg;
		pinit(exec);
	}

	public boolean initSuccessed=false;
	private void pinit(Executor exec) {
		onMsgRecv = new Container<EventHandler<ICommonNet, NetMessage>>();
		onConnect = new Container<EventHandler<ICommonNet, NetConnection>>();
		thread = exec;
		dsocsServer = new LinkedList<DatagramChannel>();
		dsocsClient = new LinkedList<DatagramChannel>();
		try {
			selector = Selector.open();
		} catch (IOException e) {
		}
		listenOn();
		initSuccessed=true;
	}

	private void listenOn() {
		try {
			Enumeration<NetworkInterface> itni = NetworkInterface.getNetworkInterfaces();
			LinkedList<InetAddress> iaddrs = new LinkedList<InetAddress>();
			while (itni.hasMoreElements()) {
				NetworkInterface eni = itni.nextElement();
				Enumeration<InetAddress> itia = eni.getInetAddresses();
				while (itia.hasMoreElements()) {
					InetAddress eaddr = itia.nextElement();

					if (eaddr instanceof Inet4Address) {
						if (cfg.ipv4) {
							iaddrs.add(eaddr);
						}
					} else if (eaddr instanceof Inet6Address) {
						if (cfg.ipv6) {
							iaddrs.add(eaddr);
						}
					}

				}
			}
			if (cfg.msgPort >= 0) {
				Iterator<InetAddress> itaddr = iaddrs.iterator();
				while (itaddr.hasNext()) {
					InetAddress eaddr = itaddr.next();
					DatagramChannel tds = DatagramChannel.open();
					tds.socket().bind(new InetSocketAddress(eaddr, cfg.msgPort));
					tds.socket().setBroadcast(true);
					dsocsServer.add(tds);
					dsocsClient.add(tds);
				}
				// For linux system, bind broadcast address 255.255.255.255
				try {
					InetAddress eaddr = InetAddress
							.getByAddress(new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 });
					DatagramChannel tds = DatagramChannel.open();
					tds.socket().bind(new InetSocketAddress(eaddr, cfg.msgPort));
					tds.socket().setBroadcast(true);
					dsocsServer.add(tds);
				} catch (BindException e) {
					// Happen on windows
				}
			}
			if (cfg.msgPort >= 0) {
				anyssoc = ServerSocketChannel.open();
				anyssoc.socket().bind(new InetSocketAddress(cfg.msgPort));
			}
			selectorT = new SelectThread();
			thread.execute(selectorT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMessage(NetMessage packet, NetAddress addr) {
		if (addr == null) {
			try {
				addr = new DefaultCommonNetAddress(new InetSocketAddress(
						InetAddress.getByAddress(
								new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 }), cfg.msgPort));
			} catch (UnknownHostException e) {
			}
		}
		InetSocketAddress socAddr;
		if (addr instanceof DefaultCommonNetAddress) {
			socAddr = ((DefaultCommonNetAddress) addr).getSocketAddress();
		} else {
			throw new UnsupportedAddressTypeException();
		}
		Iterator<DatagramChannel> itds = dsocsClient.iterator();
		ByteBuffer sendBuff = ByteBuffer.allocate(packet.getBuffer().remaining()+1);
		sendBuff.put(DataPacket).put(packet.getBuffer());
		((Buffer)sendBuff).flip();
		while (itds.hasNext()) {
			try {
				DatagramChannel eds = itds.next();
				((Buffer)sendBuff).mark();
				eds.send(sendBuff, socAddr);
				((Buffer)sendBuff).reset();
			} catch (IOException e1) {
			}
		}
	}

	public void broadcastMessage(NetMessage nmsg) {
		sendMessage(nmsg, null);
	}

	public NetConnection connectTo(NetAddress addr) throws IOException {
		Socket s = SocketChannel.open().socket();
		if (addr instanceof DefaultCommonNetAddress) {
			s.connect(((DefaultCommonNetAddress) addr).getSocketAddress(), 1000);
		} else {
			throw new IOException("Unsupported address");
		}
		if (s.isConnected()) {
			SocketConnection conn = new SocketConnection();
			conn.useSocket(s, false);
			OutputStream out = conn.write();
			out.write(1);
			ByteBuffer bb = ByteBuffer.allocate(2);
			bb.order(ByteOrder.BIG_ENDIAN);
			bb.putShort((short) cfg.msgPort);
			out.write(bb.array());
			return conn;
		}
		s.close();
		return null;
	}

	private Container<EventHandler<ICommonNet, NetMessage>> onMsgRecv;

	public Container<EventHandler<ICommonNet, NetMessage>> getOnMsgRecv() {
		return onMsgRecv;
	}

	private Container<EventHandler<ICommonNet, NetConnection>> onConnect;

	public Container<EventHandler<ICommonNet, NetConnection>> getOnConnect() {
		return onConnect;
	}

	public Iterable<NetAddress> getHostAddressList() {
		LinkedList<NetAddress> addrs = new LinkedList<NetAddress>();
		try {
			for (DatagramChannel echan : dsocsClient) {
				addrs.add(new DefaultCommonNetAddress(((InetSocketAddress)echan.socket().getLocalSocketAddress())));
			}
			return addrs;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private void msgRecv(ByteBuffer bb, java.net.SocketAddress address) {
		if (onMsgRecv.get() != null) {
			if (filterPackage((InetSocketAddress) address)) {
				NetMessage lastMsg = new NetMessage();
				NetAddress asrc;
				if(bb.remaining()>1) {
					byte packetType = bb.get();
					if(packetType==DataPacket) {
						asrc=new DefaultCommonNetAddress((InetSocketAddress) address);
						lastMsg.setSource(asrc);
						lastMsg.setBuffer(bb);
						onMsgRecv.get().handle(this, lastMsg);
					}else if(packetType==CommandPacket){
						ByteBuffer bb2=ByteBuffer.allocate(1);
						bb2.put(CommandResponsePacket);
						for(DatagramChannel eds:dsocsClient) {
							try {
								((Buffer)bb2).mark();
								eds.send(bb2, address);
								((Buffer)bb2).reset();
							}catch(IOException e) {
							}
						}
					}
				}
			}
		}
	}

	private void connected(Socket s) {
		if (onConnect.get() != null) {
			SocketConnection lastConn = new SocketConnection();
			lastConn.useSocket(s, true);
			try {
				lastConn.setTimeout(1000);
				InputStream in = lastConn.read();
				int versionFlag = in.read();
				if(versionFlag!=1) {
					lastConn.close();
				}else {
					ByteBuffer bb=ByteBuffer.allocate(2);
					bb.order(ByteOrder.BIG_ENDIAN);
					in.read(bb.array());
					int port=bb.getShort()&0xffff;
					DefaultCommonNetAddress newAddr = new DefaultCommonNetAddress(
							new InetSocketAddress(lastConn.sock.getInetAddress(),port));
					lastConn.connectedAddress=newAddr;
				}
			} catch (IOException e) {
			}
			onConnect.get().handle(this, lastConn);
		} else {
			try {
				s.close();
			} catch (IOException e) {
			}
		}
	}

	private boolean filterPackage(java.net.SocketAddress addr) {
		if (cfg.filterSelf) {
			Iterator<DatagramChannel> itds = dsocsClient.iterator();
			while (itds.hasNext()) {
				java.net.SocketAddress eaddr;
				eaddr = itds.next().socket().getLocalSocketAddress();
				if (eaddr.equals(addr)) {
					return false;
				}
			}
		}
		return true;
	}

	private class SelectThread implements Runnable {
		@Override
		public void run() {
			try {
				for (DatagramChannel echan : dsocsServer) {
					echan.configureBlocking(false);
					echan.register(selector, SelectionKey.OP_READ);
				}
				if (anyssoc != null) {
					anyssoc.configureBlocking(false);
					anyssoc.register(selector, SelectionKey.OP_ACCEPT);
				}
				while (!stopped) {
					selector.select();
					Set<SelectionKey> keys = selector.selectedKeys();
					Iterator<SelectionKey> itkey = keys.iterator();
					while (itkey.hasNext()) {
						SelectionKey ekey = itkey.next();
						itkey.remove();
						if (ekey.isReadable()) {
							ByteBuffer bb = ByteBuffer.allocate(cfg.messageBufferSize);
							java.net.SocketAddress addr = ((DatagramChannel) ekey.channel()).receive(bb);
							((Buffer)bb).flip();
							msgRecv(bb, addr);
							continue;
						}
						if (ekey.isAcceptable()) {
							connected(((ServerSocketChannel) ekey.channel()).accept().socket());
							continue;
						}
					}
				}
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClosedSelectorException e) {
			}
		}

	}

	@Override
	public int getMessageBufferSize() {
		return cfg.messageBufferSize;
	}

	@Override
	public void setMessageBufferSize(int size) {
		cfg.messageBufferSize = size;
	}

	@Override
	public void updateNetStatus() throws IOException {
		close();
		pinit(thread);
	}

	@Override
	public void close() throws IOException {
		if(!initSuccessed)return;
		stopped = true;
		try {
			selector.wakeup();
			Iterator<DatagramChannel> itds = dsocsServer.iterator();
			while (itds.hasNext()) {
				try {
					itds.next().close();
				}catch(IOException e) {}
			}
			itds = dsocsClient.iterator();
			while (itds.hasNext()) {
				DatagramChannel chan = itds.next();
				if (chan.isOpen()) {
					try {
						chan.close();
					}catch(IOException e) {}
				}
			}
			try {
				anyssoc.close();
			}catch(IOException e) {}
			try {
				selector.close();
			}catch(IOException e) {
			};
		}finally {
		}
	}
}
