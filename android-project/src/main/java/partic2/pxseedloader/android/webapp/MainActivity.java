package partic2.pxseedloader.android.webapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Window;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import partic2.pxseedloader.android.launcher.ApiServer;
import xplatj.javaplat.partic2.util.PlatCoreConfig;


public class MainActivity extends Activity {

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
            this.startupUrl="http://127.0.0.1:2081";
        }
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
        wv.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });
        wv.getSettings().setMediaPlaybackRequiresUserGesture(false);
        wv.getSettings().setAllowFileAccess(true);
        wv.getSettings().setAllowContentAccess(true);
        wv.getSettings().setAllowUniversalAccessFromFileURLs(true);
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
        super.onDestroy();
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ApiServer.onActivityResult(requestCode,resultCode,data);
        super.onActivityResult(requestCode,resultCode,data);
    }
}
