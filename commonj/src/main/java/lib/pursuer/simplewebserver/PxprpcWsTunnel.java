package lib.pursuer.simplewebserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;
;
import pxprpc.base.AbstractIo;
import pxprpc.base.ServerContext;
import pxprpc.base.Utils;

public class PxprpcWsTunnel extends WebSocket implements AbstractIo {
    protected ServerContext serv;
    protected IHTTPSession req;
    protected Exception err;
    protected BlockingQueue<WebSocketFrame> wsMsg=new ArrayBlockingQueue<WebSocketFrame>(16);

    public PxprpcWsTunnel(IHTTPSession handshakeRequest) {
        super(handshakeRequest);
        this.req=handshakeRequest;
    }

    @Override
    protected void onOpen() {
        String uri = req.getUri();
        String name = uri.substring(1);

        this.serv=new ServerContext();
        this.serv.init(this,null);
        new Thread((new Runnable() {
            @Override
            public void run() {
                try {
                    PxprpcWsTunnel.this.serv.serve();
                } catch (Exception e) {
                    PxprpcWsTunnel.this.closeQuietly(CloseCode.InternalServerError, e.toString(), false);
                }
            }
        })).start();

    }

    @Override
    protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
        if (serv!=null) {
            try {
                serv.close();
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
        try {
			this.wsMsg.offer(message,1,TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        	this.closeQuietly(CloseCode.InternalServerError, "server not response too long.", false);
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

    protected byte[] readingData;
    protected int readingPos;

    @Override
    public void send(ByteBuffer[] buffs) throws IOException {
        int size=0;
        for(int i=0;i<buffs.length;i++){
            size+=buffs[i].remaining();
        }
        ByteBuffer buf=ByteBuffer.allocate(size);
        for(int i=0;i<buffs.length;i++){
            buf.put(buffs[i]);
        }
        this.send(buf.array());
    }

    @Override
    public void receive(ByteBuffer[] buffs) throws IOException {
        try {
            ArrayList<byte[]> packets=new ArrayList<byte[]>();
            WebSocketFrame wsFrm=null;
            do{
                wsFrm=wsMsg.take();
                packets.add(wsFrm.getBinaryPayload());
            }while(!wsFrm.isFin());
            int size=0;
            for(byte[] p:packets){
                size+=p.length;
            }
            ByteBuffer msg=ByteBuffer.allocate(size);
            for(byte[] p:packets){
                msg.put(p);
            }
            Utils.setPos(msg,0);
            Utils.setLimit(msg,0);
            for(int i=0;i<buffs.length-1;i++){
                Utils.setLimit(msg,msg.position()+buffs[i].remaining());
                int bufStart=buffs[i].position();
                buffs[i].put(msg);
                Utils.setPos(buffs[i],bufStart);
            }
            Utils.setLimit(msg,msg.capacity());
            buffs[buffs.length-1]=msg;
        } catch (InterruptedException e) {
        }
    }

    @Override
	public void close() {
		if(err!=null) {
			this.closeQuietly(CloseCode.InternalServerError,err.toString(), false);
		}
	}

}
