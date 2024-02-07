package project.xplat.webapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import lib.pursuer.simplewebserver.PxprpcWsServer;
import lib.pursuer.simplewebserver.XplatHTTPDServer;
import org.nanohttpd.protocols.http.NanoHTTPD;


import project.xplat.launcher.AssetsCopy;
import project.xplat.launcher.ApiServer;
import pxprpc.base.ServerContext;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.pursuer.util.IFactory;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    void bgThread() {
        ApiServer.start(this);
        initWebServer();
    }
    static NanoHTTPD httpd;
    static int httpdPort = 2080;
    public void initWebServer() {
        try {
            if (httpd == null) {
                project.xplat.launcher.MainActivity.ensureStartOpts();
                if(project.xplat.launcher.MainActivity.debugMode){
                    httpd = new XplatHTTPDServer("0.0.0.0", httpdPort);
                }else{
                    httpd = new XplatHTTPDServer("127.0.0.1", httpdPort);
                }
                PxprpcWsServer.registeredServer.put(Integer.toString(ApiServer.port), new IFactory<ServerContext>() {
					@Override
					public ServerContext create() {
						ServerContext sc=new ServerContext();
						sc.funcMap=ApiServer.tcpServ.funcMap;
						return sc;
					}
				});
                httpd.start(60 * 1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openSystemWebBrowser(String url) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        startActivity(intent);
    }

    protected WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //ensure this is called when every activity created
        AssetsCopy.init(this);
        PlatCoreConfig.get().executor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.bgThread();
                    }
                }
        );
        initWebView();
    }

    protected void initWebView() {
        mWebView = new WebView(this);
        setContentView(mWebView);
        mWebView.getSettings().setDefaultTextEncodingName("utf-8");
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        try {
            mWebView.loadUrl("http://127.0.0.1:" + httpdPort +XplatHTTPDServer.urlPathForFile(new File(AssetsCopy.assetsDir + "/index.html")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onDestroy() {

       PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                httpd.stop();
                httpd = null;
            }
        });
        ApiServer.stop();

        super.onDestroy();
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ApiServer.onActivityResult(requestCode,resultCode,data);
    }
}
