package lib.pursuer.simplewebserver;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.websockets.NanoWSD;
import org.nanohttpd.protocols.websockets.WebSocket;


public class WebSocketTunnelServer extends NanoWSD {

    public WebSocketTunnelServer(int port) {
        super(port);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new WebSocketTunnel(handshake);
    }
}
