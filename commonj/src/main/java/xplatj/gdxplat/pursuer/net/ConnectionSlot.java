package xplatj.gdxplat.pursuer.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.gdxplat.pursuer.utils.Env;
import xplatj.javaplat.pursuer.io.stream.PackageIOStream;
import xplatj.javaplat.pursuer.net.NetAddress;
import xplatj.javaplat.pursuer.net.NetConnection;
import xplatj.javaplat.pursuer.net.NetMessage;
import xplatj.javaplat.pursuer.net.ICommonNet;
import xplatj.javaplat.pursuer.net.NetStringMessage;
import xplatj.javaplat.pursuer.util.Container;
import xplatj.javaplat.pursuer.util.EventHandler;
import xplatj.javaplat.pursuer.util.EventListener2;


public class ConnectionSlot implements Closeable {
	protected Map<String, EventHandler<ConnectionSlot, NetConnection>> onReqCall = new HashMap<String, EventHandler<ConnectionSlot, NetConnection>>();;
	protected ICommonNet nmgr;
	public static final String AckFlag = "ack";
	public static final String DenyFlag = "deny";

	protected EventListener2<ICommonNet, NetConnection> connectListener;
	protected EventListener2<ICommonNet, NetMessage> messageListener;

	private void initDefault() {
		nmgr = PlatCoreConfig.get().getNetManager();
		connectListener = new EventListener2<ICommonNet, NetConnection>(nmgr.getOnConnect()) {
			public void run() {
				PlatCoreConfig.get().executor.execute(new EventOnReq(getData()));
			}
		};
		messageListener = new EventListener2<ICommonNet, NetMessage>(nmgr.getOnMsgRecv()) {
			@Override
			public void run() {
				PlatCoreConfig.get().executor.execute(new EventOnMsg(getData()));
			}
		};
		Env.ss(ConnectionSlot.class, this);
	}

	public ConnectionSlot() {
		initDefault();
	}

	public ConnectionSlot(boolean initDefult) {
		if (initDefult) {
			initDefault();
		}
	}

	public ConnectionSlot(ICommonNet nmgr) {
		this.nmgr = nmgr;
	}

	public NetConnection connect(NetAddress addr, String reqSrvName) throws IOException {
		if (reqSrvName.startsWith("\\")) {
			throw new IOException("Prefix character \"\\\" is reserverd.");
		}
		NetConnection conn = null;

		try {
			conn = nmgr.connectTo(addr);
			conn.setTimeout(3000);
			PackageIOStream pio = new PackageIOStream(conn.read(), conn.write());
			NetStringMessage msg = new NetStringMessage();
			msg.setMessage(reqSrvName);
			pio.sendPackage(msg.getData());
			msg.setData(pio.waitPackage());

			if (AckFlag.equals(msg.getMessage())) {
				return conn;
			} else {
				throw new ProtocolException("Server response " + msg.getMessage());
			}
		} catch (IOException e) {
			if (conn != null) {
				try {
					conn.close();
				} catch (IOException e1) {
				}
			}
			throw e;
		}
	}

	public void onConnected(String srvName, EventHandler<ConnectionSlot, NetConnection> onReq) {
		if (onReq == null) {
			onReqCall.remove(srvName);
		} else {
			onReqCall.put(srvName, onReq);
		}
	}

	private class EventOnReq implements Runnable {
		private NetConnection conn;

		public EventOnReq(NetConnection conn) {
			this.conn = conn;
		}

		@Override
		public void run() {
			try {
				conn.setTimeout(3000);
				PackageIOStream pio = new PackageIOStream(conn.read(), conn.write());
				NetStringMessage msg = new NetStringMessage();
				msg.setData(pio.waitPackage());
				EventHandler<ConnectionSlot, NetConnection> callback = onReqCall.get(msg.getMessage());
				if (callback != null) {
					msg.setMessage(AckFlag);
					pio.sendPackage(msg.getData());
					callback.handle(ConnectionSlot.this, conn);
				} else {
					msg.setMessage(DenyFlag);
					pio.sendPackage(msg.getData());
					conn.close();
				}
			} catch (IOException e) {
				try {
					conn.close();
				} catch (IOException e1) {
				}
			}
		}
	}

	private class EventOnMsg implements Runnable {
		NetMessage msg;

		public EventOnMsg(NetMessage data) {
			this.msg=data;
		}

		@Override
		public void run() {
			NetStringMessage nsm = new NetStringMessage(msg);
			String m = nsm.getMessage();
			if (m != null) {
				if (m.startsWith("ConnectionSlot.query:")) {
					String querySrv = m.substring("ConnectionSlot.query:".length());
					NetAddress src = msg.getSource();
					if (onReqCall.containsKey(querySrv)) {
						nmgr.sendMessage(new NetStringMessage("ConnectionSlot.found:" + querySrv), src);
					} else {
						nmgr.sendMessage(new NetStringMessage("ConnectionSlot.notFound:" + querySrv), src);
					}
				}
			}
		}
	}

	private static class QPHandler extends Container<EventHandler<QueryProgress, Object>> {
	};

	public static class QueryProgress implements Closeable {
		private QPHandler onNewResponse = new QPHandler();
		public Collection<NetAddress> servFoundAddr = new LinkedList<NetAddress>();
		public Collection<NetAddress> servNotFoundAddr = new LinkedList<NetAddress>();
		private ICommonNet cn;
		private EventListener2<ICommonNet, NetMessage> listener;
		private String srvName;
		class OnRespRecv implements Runnable{
			NetMessage msg;
			OnRespRecv(NetMessage msg){
				this.msg=msg;
			}
			@Override
			public void run() {
				NetStringMessage smsg = new NetStringMessage(msg);
				String msgStr = smsg.getMessage();
				if (msgStr.startsWith("ConnectionSlot.")) {
					if (msgStr.equals("ConnectionSlot.found:"+srvName)) {
						synchronized (servFoundAddr) {
							servFoundAddr.add(msg.getSource());
						}
						fireEvent();
					} else if (msgStr.equals("ConnectionSlot.notFound:"+srvName)) {
						synchronized (servNotFoundAddr) {
							servNotFoundAddr.add(msg.getSource());
						}
						fireEvent();
					}
				}
			}
		}
		public void start() {
			listener=new EventListener2<ICommonNet, NetMessage>(cn.getOnMsgRecv()) {
				@Override
				public void run() {
					PlatCoreConfig.get().executor.execute(new OnRespRecv(getData()));
					super.run();
				}
			};
			cn.sendMessage(new NetStringMessage("ConnectionSlot.query:"+srvName), null);
		}

		public QueryProgress(ICommonNet cn, String srvName) {
			this.cn = cn;
			this.srvName=srvName;
			
		}
		protected void fireEvent() {
			EventHandler<QueryProgress, Object> handler = getOnNewResponse().get();
			if(handler!=null)
				handler.handle(this, null);
		}

		public Container<EventHandler<QueryProgress, Object>> getOnNewResponse() {
			return onNewResponse;
		}

		@Override
		public void close() throws IOException {
			if(listener!=null) {
				listener.cancelAllListen();
			}
		}
		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}
	}

	public QueryProgress queryServ(String srvName) {
		return new QueryProgress(nmgr, srvName);
	}
	
	public Collection<NetAddress> queryrCertainAmountServ(String srvName,final int amount,int timeout,TimeUnit unit) throws InterruptedException{
		QueryProgress progress = queryServ(srvName);
		try {
			final Container<Collection<NetAddress>> result = new Container<Collection<NetAddress>>();
			progress.getOnNewResponse().set(new EventHandler<ConnectionSlot.QueryProgress, Object>() {
				@Override
				public void handle(QueryProgress from, Object data) {
					if(from.servFoundAddr.size()>=amount) {
						synchronized(result) {
							result.set(from.servFoundAddr);
							result.notify();
						}
					}
				}
			});
			progress.start();
			synchronized (result) {
				if(result.get()==null)
					result.wait(unit.toMillis(timeout));
			}
			return progress.servFoundAddr;
		}finally {
			try {
				progress.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (connectListener != null) {
			connectListener.cancelAllListen();
		}
		if (messageListener != null) {
			messageListener.cancelAllListen();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
}
