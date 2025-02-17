package lib.pursuer.simplewebserver;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.websockets.NanoWSD;
import org.nanohttpd.webserver.SimpleWebServer;
import xplatj.gdxconfig.core.PlatCoreConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class XplatHTTPDServer extends NanoHTTPD {
    protected NanoWSD wsServ;
    protected PxprpcWsServer pxprpcServ;
    protected CorsBusterServer corsBuster;
    public Map<String,SimpleWebServer> fileServ=new HashMap<>();

    public XplatHTTPDServer(String hostname, int port) {
        super(hostname, port);
        try {
            for(File f:File.listRoots()){
                    String path=f.getCanonicalPath();
                    if(path.endsWith("/")||path.endsWith("\\")){
                        fileServ.put(path.substring(0,path.length()-1),new SimpleWebServer(hostname,port,f,true,"*"));
                    }else{
                        fileServ.put(path,new SimpleWebServer(hostname,port,f,true,"*"));
                    }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        wsServ=new WebSocketTunnelServer(port);
        pxprpcServ=new PxprpcWsServer(port);
        corsBuster=new CorsBusterServer();
    }

    public static String fileServerPrefix="/localFile";
    public static String websocketTunnelPrefix="/websocketTunnel";
    public static String pxprpcWsTunnelPrefix="/pxprpc";
    public static String corsBusterPrefix="/corsBuster";

    public static String urlPathForFile(File f) throws IOException {
        String path = f.getCanonicalPath();
        if(path.startsWith("/")){
            return fileServerPrefix+path.replace("\\","/");
        }else{
            return fileServerPrefix+"/"+path.replace("\\","/");
        }
    }
    public static File fileForUrlPath(String urlPath) throws IOException {
        if(!urlPath.startsWith(fileServerPrefix)){
            return null;
        }
        File[] fileRoot = File.listRoots();
        boolean win32=true;
        for(File f:File.listRoots()){
            String name = f.getCanonicalPath();
            if(name.equals("/")){
                win32=false;
            }
        }
        if(!win32){
            return new File(urlPath.substring(fileServerPrefix.length()));
        }else{
            return new File(urlPath.substring(fileServerPrefix.length()+1).replace("/",File.separator));
        }
    }
    @Override
    public Response handle(IHTTPSession session) {
        String uri = session.getUri();
        if(uri.startsWith(fileServerPrefix)){
            if(fileServ.containsKey("")){
                return fileServ.get("").handle(new ProxyIHTTPSession(){
                    @Override
                    public String getUri() {
                        String originUrl=super.getUri();
                        return originUrl.substring(fileServerPrefix.length());
                    }
                }.wrap(session));
            }else{
                //usually on windows
                int startAt=uri.indexOf("/",2);
                final int endAt=uri.indexOf("/",startAt+1);
                String rootName=uri.substring(startAt+1,endAt);
                SimpleWebServer selectedServ = fileServ.get(rootName);
                if(selectedServ!=null){
                    return selectedServ.handle(new ProxyIHTTPSession(){
                        @Override
                        public String getUri() {
                            String originUrl=super.getUri();
                            return originUrl.substring(endAt);
                        }
                    }.wrap(session));
                }else{
                    return Response.newFixedLengthResponse(Status.NOT_FOUND,"text/plain","No such file root");
                }
            }
        }else if(uri.startsWith(pxprpcWsTunnelPrefix)) {
        	return pxprpcServ.handle(new ProxyIHTTPSession(){
                @Override
                public String getUri() {
                    String originUrl=super.getUri();
                    return originUrl.substring(pxprpcWsTunnelPrefix.length());
                }
            }.wrap(session));
        }else if(uri.startsWith(websocketTunnelPrefix)){
            return wsServ.handle(new ProxyIHTTPSession(){
                @Override
                public String getUri() {
                    String originUrl=super.getUri();
                    return originUrl.substring(websocketTunnelPrefix.length());
                }
            }.wrap(session));
        }else if(uri.startsWith(corsBusterPrefix)){
            return corsBuster.handle(session);
        }else{
            return super.handle(session);
        }
    }

}
