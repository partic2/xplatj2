package lib.pursuer.simplewebserver;


import java.util.HashMap;
import java.util.Map;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.websockets.NanoWSD;
import org.nanohttpd.protocols.websockets.WebSocket;

import pursuer.pxprpc.ServerContext;
import xplatj.javaplat.pursuer.util.IFactory;


public class PxprpcWsServer extends NanoWSD {

	//Bad scalability, but it's convenient.
    public static HashMap<String,IFactory<ServerContext>> registeredServer=new HashMap<String,IFactory<ServerContext>>();
    
    public PxprpcWsServer(int port) {
        super(port);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new PxprpcWsTunnel(handshake);
    }
}
