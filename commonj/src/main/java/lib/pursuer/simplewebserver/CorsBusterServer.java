package lib.pursuer.simplewebserver;

import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import xplatj.javaplat.partic2.util.CloseableGroup;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CorsBusterServer {
    static HashSet<String> noProxyHeader=new HashSet<String>();
    static {
        noProxyHeader.add("host");
    }
    public Response handle(IHTTPSession sessionIn) {
        HTTPSession session = (HTTPSession) sessionIn;
        CloseableGroup closables=new CloseableGroup();
        try{
            String path=session.getUri();
            int delim1=path.indexOf("/",1);
            //First part is corsBuster prefix
            path=path.substring(delim1+1);
            delim1=path.indexOf("/");
            String host=path.substring(0,delim1);
            try {
                host=URLDecoder.decode(host,"utf-8");
            } catch (UnsupportedEncodingException e) {
            }
            path=path.substring(delim1);
            URL realUrl = null;
            realUrl = new URL(host + path);
            HttpURLConnection conn = (HttpURLConnection)realUrl.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod(session.getMethod().name());
            ArrayList<Map.Entry<String, String>> laterProcessHeaders=new ArrayList<>();
            for(Map.Entry<String, String> header:session.getHeaders().entrySet()){
                if(noProxyHeader.contains(header.getKey().toLowerCase())){
                    continue;
                }
                if(header.getKey().toLowerCase().startsWith("cors-buster-header-")){
                    laterProcessHeaders.add(header);
                    continue;
                }
                conn.setRequestProperty(header.getKey(),header.getValue());
            }
            int t1="cors-buster-header-".length();
            for(Map.Entry<String, String> header:laterProcessHeaders){
                conn.setRequestProperty(header.getKey().substring(t1),header.getValue());
            }
            OutputStream output1 = conn.getOutputStream();
            closables.add(output1);
            InputStream input1=session.getInputStream();

            byte[] buffer=new byte[1024];
            long remain=session.getBodySize();
            for(t1=0;t1<10000000&&remain>0;t1++){
                int len=input1.read(buffer);
                if(len<=0)break;
                remain-=len;
                output1.write(buffer,0,len);
            }
            output1.close();
            conn.connect();
            long contentLength=conn.getContentLengthLong();
            Response resp;
            if(contentLength==-1){
                resp=Response.newChunkedResponse(
                        Status.lookup(conn.getResponseCode()),
                        conn.getContentType(),
                        conn.getInputStream());
            }else{
                resp=Response.newFixedLengthResponse(
                        Status.lookup(conn.getResponseCode()),
                        conn.getContentType(),
                        conn.getInputStream(),contentLength);
            }
            for(Map.Entry<String, List<String>> header:conn.getHeaderFields().entrySet()){
                for(String val:header.getValue()){
                    resp.addHeader(header.getKey(),val);
                }
            }
            resp.addHeader("Access-Control-Allow-Origin","*");
            resp.addHeader("Access-Control-Allow-Methods","*");
            return resp;
        }catch(Exception ex){
            Response resp = Response.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", ex.toString());
            resp.addHeader("Access-Control-Allow-Origin","*");
            resp.addHeader("Access-Control-Allow-Methods","*");
            return resp;
        }finally {
            closables.closeQuietly();
        }
    }
}
