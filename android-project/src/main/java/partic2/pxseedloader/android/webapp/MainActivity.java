package partic2.pxseedloader.android.webapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import lib.pursuer.simplewebserver.XplatHTTPDServer;
import org.nanohttpd.protocols.http.NanoHTTPD;


import partic2.pxseedloader.android.launcher.ApiServer;
import partic2.pxseedloader.android.launcher.AssetsCopy;
import xplatj.javaplat.partic2.util.PlatCoreConfig;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class MainActivity extends Activity {
    protected void bgThread() {
        initWebServer();
    }
    public static volatile NanoHTTPD httpd;
    public static int httpdPort = 2080;
    public static int[] httpdPortRange=new int[]{2080,2095};
    public void initWebServer() {
        try {
            if (httpd == null) {
                partic2.pxseedloader.android.launcher.MainActivity.ensureStartOpts();

                String hostname="127.0.0.1";
                if(PlatCoreConfig.debugMode){
                    hostname="0.0.0.0";
                }
                //check port available
                for(httpdPort=httpdPortRange[0];httpdPort<httpdPortRange[1];httpdPort++){
                    try{
                        ServerSocket ss = new ServerSocket(httpdPort, 1, InetAddress.getByName(hostname));
                        ss.close();
                        break;
                    }catch(Exception ex){}
                }
                if(httpdPort>=httpdPortRange[1]){
                    throw new RuntimeException("No available tcp port.");
                }
                httpd = new XplatHTTPDServer(hostname, httpdPort);
                httpd.start(60 * 60 * 1000);
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

    public WebView mainWebView;
    public String startupUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if(PlatCoreConfig.get()==null){
            PlatCoreConfig.singleton.set(new PlatCoreConfig());
        }
        super.onCreate(savedInstanceState);
        Intent intent=this.getIntent();
        this.startupUrl=intent.getStringExtra("url");
        if(this.startupUrl==null){
            try {
                this.startupUrl="http://127.0.0.1:" + httpdPort +XplatHTTPDServer.urlPathForFile(new File(AssetsCopy.assetsDir + "/index.html"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        PlatCoreConfig.get().executor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.bgThread();
                    }
                }
        );
        initWebView();
        //PlatCoreConfig.get().executor.execute(()->TestCode.do2());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ApiServer.defaultAndroidContext=this;
    }

    protected void initWebView() {
        WebView wv=new WebView(this);
        mainWebView=wv;
        setContentView(wv);
        wv.getSettings().setDefaultTextEncodingName("utf-8");
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }


            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if(MainActivity.this.startScript!=null){
                    MainActivity.this.webviewRunJs(MainActivity.this.startScript);
                }
                super.onPageStarted(view, url, favicon);
            }


        });
        wv.getSettings().setAllowFileAccess(true);
        wv.getSettings().setAllowContentAccess(true);
        wv.getSettings().setAllowUniversalAccessFromFileURLs(true);
        while(httpd==null){
            //race condition
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
        mainWebView.loadUrl(this.startupUrl);
    }
    protected void deinitWebView(){
        WebView wv = ((WebView) mainWebView);
        if(wv!=null){
            wv.destroy();
        }
    }
    protected String startScript=null;
    public void setWebviewStartScript(String jscode){
        this.startScript=jscode;
    }
    public void webviewRunJs(String jscode){
        WebView wv = ((WebView) mainWebView);
        wv.evaluateJavascript("javascript:"+jscode,null);
    }


    @Override
    protected void onDestroy() {
        deinitWebView();
       PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                if(httpd!=null){
                    httpd.stop();
                    httpd = null;
                }
            }
        });
        super.onDestroy();
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ApiServer.onActivityResult(requestCode,resultCode,data);
        super.onActivityResult(requestCode,resultCode,data);
    }
}
