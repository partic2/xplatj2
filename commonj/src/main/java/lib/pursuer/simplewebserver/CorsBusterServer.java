package lib.pursuer.simplewebserver;

import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import xplatj.javaplat.partic2.util.CloseableGroup;

import javax.net.ssl.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CorsBusterServer {
    static String OverrideRequestHeader="override-request-";
    static String OverrideResponseHeader="override-response-";
    static HashSet<String> ignoreHeader=new HashSet<String>();
    {
        ignoreHeader.add("host");
        ignoreHeader.add("origin");
        ignoreHeader.add("cookie");
        ignoreHeader.add("referer");
        ignoreHeader.add("authorization");
    }
    {
        try{
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        }catch(Exception ex){};
    }

    public Response handle(IHTTPSession sessionIn) {
        HTTPSession session = (HTTPSession) sessionIn;
        CloseableGroup closables=new CloseableGroup();
        try{
            String path=session.getUri();
            String search=session.getQueryParameterString();
            if(search==null){
                search="";
            }else{
                search="?"+search;
            }
            int delim1=path.indexOf("/",1);
            //First part is corsBuster prefix
            path=path.substring(delim1+1);
            if(path.equals("version")){
                return Response.newFixedLengthResponse("corsBuster 1.0,nanohttpd+HttpURLConnection");
            }
            delim1=path.indexOf("/");
            String host=path.substring(0,delim1);
            try {
                host=URLDecoder.decode(host,"utf-8");
            } catch (UnsupportedEncodingException e) {
            }
            path=path.substring(delim1);
            URL realUrl = null;
            realUrl = new URL(host + path + search);
            HttpURLConnection conn = (HttpURLConnection)realUrl.openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod(session.getMethod().name());
            ArrayList<Map.Entry<String, String>> overrideRequest=new ArrayList<>();
            ArrayList<Map.Entry<String, String>> overrideResponse=new ArrayList<>();
            for(Map.Entry<String, String> header:session.getHeaders().entrySet()){
                if(ignoreHeader.contains(header.getKey().toLowerCase())){
                    continue;
                }
                if(header.getKey().toLowerCase().startsWith(OverrideRequestHeader)){
                    overrideRequest.add(header);
                    continue;
                }
                if(header.getKey().toLowerCase().startsWith(OverrideResponseHeader)){
                    overrideResponse.add(header);
                    continue;
                }
                conn.setRequestProperty(header.getKey(),header.getValue());
            }
            int t1=OverrideRequestHeader.length();
            for(Map.Entry<String, String> header:overrideRequest){
                conn.setRequestProperty(header.getKey().substring(t1),header.getValue());
            }
			
            long remain=session.getBodySize();
            if(remain>0){
				byte[] buffer=new byte[1024];
                conn.setDoOutput(true);
                OutputStream output1 = conn.getOutputStream();
                closables.add(output1);
                InputStream input1=session.getInputStream();
                for(t1=0;t1<10000000&&remain>0;t1++){
                    int len=input1.read(buffer);
                    if(len<=0)break;
                    remain-=len;
                    output1.write(buffer,0,len);
                }
                output1.close();
            }
            conn.connect();
            long contentLength=conn.getContentLengthLong();
            Response resp;
            if(conn.getResponseCode()<399){
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
            }else {
                resp = Response.newFixedLengthResponse(
                        Status.lookup(conn.getResponseCode()),
                        conn.getContentType(),
                        conn.getErrorStream(), contentLength);
            }
            for(Map.Entry<String, List<String>> header:conn.getHeaderFields().entrySet()) {
				if(header.getKey()==null){
					continue;
				}
                for (String val : header.getValue()) {
                    resp.addHeader(header.getKey(), val);
                }
            }
            resp.addHeader("Access-Control-Allow-Origin","*");
            resp.addHeader("Access-Control-Allow-Methods","*");
            t1=OverrideResponseHeader.length();
            for(Map.Entry<String, String> header:overrideResponse){
                resp.addHeader(header.getKey().substring(t1),header.getValue());
            }
			resp.setUseGzip(false);
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
