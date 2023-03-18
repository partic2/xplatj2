package lib.pursuer.simplewebserver;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.content.CookieHandler;
import org.nanohttpd.protocols.http.request.Method;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ProxyIHTTPSession implements IHTTPSession {

    protected IHTTPSession wrapped;

    public ProxyIHTTPSession wrap(IHTTPSession wrapped){
        this.wrapped=wrapped;
        return this;
    }

    @Override
    public void execute() throws IOException {
        wrapped.execute();
    }

    @Override
    public CookieHandler getCookies() {
        return wrapped.getCookies();
    }

    @Override
    public Map<String, String> getHeaders() {
        return wrapped.getHeaders();
    }

    @Override
    public InputStream getInputStream() {
        return wrapped.getInputStream();
    }

    @Override
    public Method getMethod() {
        return wrapped.getMethod();
    }

    @Override
    @Deprecated
    public Map<String, String> getParms() {
        return wrapped.getParms();
    }

    @Override
    public Map<String, List<String>> getParameters() {
        return wrapped.getParameters();
    }

    @Override
    public String getQueryParameterString() {
        return wrapped.getQueryParameterString();
    }

    @Override
    public String getUri() {
        return wrapped.getUri();
    }

    @Override
    public void parseBody(Map<String, String> files) throws IOException, NanoHTTPD.ResponseException {
        wrapped.parseBody(files);
    }

    @Override
    public String getRemoteIpAddress() {
        return wrapped.getRemoteIpAddress();
    }



}
