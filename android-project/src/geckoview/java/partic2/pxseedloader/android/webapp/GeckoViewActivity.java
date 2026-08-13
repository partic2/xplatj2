package partic2.pxseedloader.android.webapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Window;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

import partic2.pxseedloader.android.launcher.ApiServer;
import pxprpcapi.androidhelper.AndroidUIBase;
import xplatj.javaplat.partic2.util.PlatCoreConfig;


public class GeckoViewActivity extends Activity {
    public static GeckoRuntime geckoRuntime;

    public void openSystemWebBrowser(String url) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        startActivity(intent);
    }

    public GeckoView mainWebView;
    public GeckoSession webviewSession;
    public String startupUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if(PlatCoreConfig.get()==null){
            PlatCoreConfig.singleton.set(new PlatCoreConfig());
        }
        super.onCreate(savedInstanceState);
        if(geckoRuntime==null){
            geckoRuntime=GeckoRuntime.create(this.getApplicationContext());
        }
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
        if(AndroidUIBase.i!=null){
            AndroidUIBase.i.extraEvent.fireEvent("webapp.onResume");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(ApiServer.defaultAndroidContext==this){
            ApiServer.defaultAndroidContext=getApplicationContext();
        }
        if(AndroidUIBase.i!=null){
            AndroidUIBase.i.extraEvent.fireEvent("webapp.onStop");
        }
    }

    protected void initWebView() {
        this.webviewSession=new GeckoSession();
        this.webviewSession.setNavigationDelegate(new GeckoSession.NavigationDelegate() {

        });
        this.webviewSession.open(geckoRuntime);
        GeckoView wv=new GeckoView(this);
        wv.setSession(this.webviewSession);
        mainWebView=wv;
        setContentView(wv);
        this.webviewSession.loadUri(this.startupUrl);
    }
    protected void deinitWebView(){
        this.webviewSession.open(geckoRuntime);

    }
    protected String startScript=null;
    public void setWebviewStartScript(String jscode){
        throw new RuntimeException("Not implemented");
    }
    public void webviewRunJs(String jscode){
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void onBackPressed() {
        if(AndroidUIBase.i!=null){
            AndroidUIBase.i.extraEvent.fireEvent("backPressed");
        }
        if(!AndroidUIBase.interceptBackPressed){
            super.onBackPressed();
            this.finish();
        }
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
