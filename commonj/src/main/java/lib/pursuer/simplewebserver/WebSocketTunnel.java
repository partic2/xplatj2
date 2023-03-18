package lib.pursuer.simplewebserver;


import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.OpCode;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class WebSocketTunnel extends WebSocket implements Runnable {
    protected SocketChannel soc;
    protected IHTTPSession req;

    public WebSocketTunnel(IHTTPSession handshakeRequest) {
        super(handshakeRequest);
        this.req=handshakeRequest;
    }

    @Override
    protected void onOpen() {
        String uri = req.getUri();
        String[] uri2 = uri.substring(1).split("/");
        int port = 0;
        String ip = "localhost";
        try {
            if (uri2.length == 1) {
                port = Integer.parseInt(uri2[0]);
            } else if (uri2.length == 2) {
                ip = URLDecoder.decode(uri2[0], "utf-8");
                port = Integer.parseInt(uri2[1]);
            }
            soc=SocketChannel.open(new InetSocketAddress(InetAddress.getByName(ip), port));

            new Thread(this).start();
        } catch (IOException e) {
            this.closeQuietly(CloseCode.InternalServerError, e.toString(), false);
        }
    }

    @Override
    protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
        if (soc!=null && soc.isOpen()) {
            try {
                soc.close();
            } catch (IOException e) {
            }
        }
    }

    protected void closeQuietly(CloseCode code, String reason, boolean initiatedByRemote) {
        try {
            this.close(code, reason, initiatedByRemote);
        } catch (IOException e) {
        }
    }

    @Override
    protected void onMessage(WebSocketFrame message) {
        byte[] data = message.getBinaryPayload();
        try {
            soc.write(ByteBuffer.wrap(data));
        } catch (IOException e) {
            this.closeQuietly(CloseCode.InternalServerError, e.toString(), false);
        }
    }

    @Override
    protected void onPong(WebSocketFrame pong) {
    }

    @Override
    protected void onException(IOException exception) {
        try {
            this.close(CloseCode.InternalServerError, exception.toString(), false);
        } catch (IOException e) {
        }
    }

    @Override
    public void run() {
        ByteBuffer bb = ByteBuffer.allocate(4096);
        try {
            while (soc.isOpen() && this.isOpen()) {
                if (this.soc.read(bb) < 0) {
                    //eof
                    break;
                }
                byte[] data = new byte[((Buffer) bb).flip().remaining()];
                if (data.length > 0) {
                    bb.get(data);
                    this.send(data);
                }
                ((Buffer)bb).clear();
            }
            this.closeQuietly(CloseCode.NormalClosure, "normal closed", true);
        } catch (IOException e) {
            this.closeQuietly(CloseCode.InternalServerError, e.toString(), true);
        } finally {
            try {
                if (this.soc.isOpen()) this.soc.close();
            } catch (IOException e) {
            }

        }
    }
}
