package lib.pursuer.simplewebserver;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.websockets.NanoWSD;
import org.nanohttpd.webserver.SimpleWebServer;
import xplatj.gdxconfig.core.PlatCoreConfig;

import java.io.File;

public class XplatHTTPDServer extends NanoHTTPD {
    protected SimpleWebServer fileServ;
    protected NanoWSD wsServ;
    protected PxprpcWsServer pxprpcServ;

    public XplatHTTPDServer(String hostname, int port,File wwwroot) {
        super(hostname, port);
        fileServ=new SimpleWebServer("localhost",port,wwwroot,true,"*");
        wsServ=new WebSocketTunnelServer(port);
        pxprpcServ=new PxprpcWsServer(port);
    }

    public static String fileServerPrefix="/localFile";
    public static String websocketTunnelPrefix="/websocketTunnel";
    public static String pxprpcWsTunnelPrefix="/pxprpc";
    @Override
    public Response handle(IHTTPSession session) {
        String uri = session.getUri();
        if(uri.startsWith(websocketTunnelPrefix)){
            return wsServ.handle(new ProxyIHTTPSession(){
                @Override
                public String getUri() {
                    String originUrl=super.getUri();
                    return originUrl.substring(websocketTunnelPrefix.length());
                }
            }.wrap(session));
        }else if(uri.startsWith(fileServerPrefix)){
            return fileServ.handle(new ProxyIHTTPSession(){
                @Override
                public String getUri() {
                    String originUrl=super.getUri();
                    return originUrl.substring(fileServerPrefix.length());
                }
            }.wrap(session));
        }else if(uri.startsWith(pxprpcWsTunnelPrefix)) {
        	return pxprpcServ.handle(new ProxyIHTTPSession(){
                @Override
                public String getUri() {
                    String originUrl=super.getUri();
                    return originUrl.substring(pxprpcWsTunnelPrefix.length());
                }
            }.wrap(session));
        }else{
            return super.handle(session);
        }
    }

}
