package pxprpcapi.androidhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import partic2.pxseedloader.android.launcher.ApiServer;
import partic2.pxseedloader.android.webapp.MainActivity;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.EventDispatcher;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

public class AndroidUIBase implements Closeable {
    public static final String PxprpcNamespace="AndroidHelper.AndroidUIBase";
    public static AndroidUIBase i;


    public AndroidUIBase(){
        i=this;
        this.extraEvent=new EventDispatcher();
        this.extraEvent.setEventType(String.class);
    }
    public void dispatchKeyEvent(int action,int code){
        new Handler(ApiServer.defaultAndroidContext.getMainLooper()).post(()-> {
            if(!(ApiServer.defaultAndroidContext instanceof Activity)){
                return;
            }
            Activity activity = (Activity) ApiServer.defaultAndroidContext;
            KeyEvent keyEvent = new KeyEvent(action, code);
            activity.dispatchKeyEvent(keyEvent);
        });
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
        try{
            this.extraEvent.close();
        }catch(Exception ex){}
        try{
            this.dialogEvent.close();
        }catch(Exception ex){}
    }

    public static class TouchPointers{
        MotionEvent.PointerProperties[] props;
        MotionEvent.PointerCoords[] coords;
    }
    public TouchPointers createTouchPointer(ByteBuffer init){
        TouchPointers r = new TouchPointers();
        if(init.remaining()==0)return r;
        TableSerializer tab = new TableSerializer().load(init);
        int rowCount=tab.getRowCount();
        r.props=new MotionEvent.PointerProperties[rowCount];
        r.coords=new MotionEvent.PointerCoords[rowCount];
        for(int i=0;i<rowCount;i++){
            Object[] row = tab.getRow(i);
            r.props[i]=new MotionEvent.PointerProperties();
            r.props[i].id=((Number)row[0]).intValue();
            r.props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            r.coords[i]=new MotionEvent.PointerCoords();
            r.coords[i].x=((Number)row[1]).floatValue();
            r.coords[i].y=((Number)row[2]).floatValue();
            r.coords[i].pressure=row.length>3?((Number)row[3]).floatValue():1.0f;
            r.coords[i].size=1.0f;
        }
        return r;
    }
    public void dispatchTouchEvent(int action,TouchPointers touchPointers){
        if(touchPointers==null || touchPointers.coords==null || touchPointers.coords.length==0){
            return;
        }
        new Handler(ApiServer.defaultAndroidContext.getMainLooper()).post(()->{
            if(!(ApiServer.defaultAndroidContext instanceof Activity)){
                return;
            }
            Activity activity = (Activity) ApiServer.defaultAndroidContext;
            MotionEvent ev = MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),
                    action,touchPointers.coords.length,touchPointers.props,touchPointers.coords,
                    0,0,1.0f,1.0f,0,0,0,0);
            try{
                activity.dispatchTouchEvent(ev);
            }finally{
                ev.recycle();
            }
        });
    }

    public void webViewSetStartScript(final String script){
        new Handler(ApiServer.defaultAndroidContext.getMainLooper()).post(()->{
            if(ApiServer.defaultAndroidContext instanceof MainActivity){
                ((MainActivity)ApiServer.defaultAndroidContext).setWebviewStartScript(script);
            }
        });
    }
    public void webViewRunJs(final String script){
        new Handler(ApiServer.defaultAndroidContext.getMainLooper()).post(()-> {
            if(ApiServer.defaultAndroidContext instanceof MainActivity){
                ((MainActivity)ApiServer.defaultAndroidContext).webviewRunJs(script);
            }
        });
    }

    public View getCurrentMainContent(){
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            throw new IllegalStateException("defaultAndroidContext is not an Activity");
        }
        Activity activity = (Activity) ApiServer.defaultAndroidContext;
        ViewGroup content=(ViewGroup)activity.findViewById(android.R.id.content);
        return content==null?null:content.getChildAt(0);
    }
    EventDispatcher dialogEvent=new EventDispatcher().setEventType(String.class);
    public EventDispatcher dialogEvent(){
        return dialogEvent;
    }
    public void dialogSet(AlertDialog dialog,String msg,boolean show){
        new Handler(ApiServer.defaultAndroidContext.getMainLooper()).post(()->{
            dialog.setMessage(msg);
            if(show && !dialog.isShowing()){
                dialog.show();
            }else if(!show && dialog.isShowing()){
                dialog.hide();
            }
        });
    }
    @MethodTypeDecl("o->c")
    public boolean dialogGet(AlertDialog dialog){
        return dialog.isShowing();
    }
    public void dialogNew(AsyncReturn<AlertDialog> aret,String btn1,String id1,String btn2,String id2){
        new Handler(ApiServer.defaultAndroidContext.getMainLooper()).post(()-> {
            Activity activity = (Activity) ApiServer.defaultAndroidContext;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("");
            if(!btn1.equals("")){
                builder.setPositiveButton(btn1,(dialog, which)->{
                    ApiServer.fireEventAsync(dialogEvent,id1);
                });
            }
            if(!btn2.equals("")){
                builder.setNegativeButton(btn2,(dialog, which)->{
                    ApiServer.fireEventAsync(dialogEvent,id2);
                });
            }
            ApiServer.resolveAsync(aret,builder.create());
        });
    }

    public static boolean interceptBackPressed=false;
    public void setIntercepptOnBackPressed(boolean b){
        AndroidUIBase.interceptBackPressed=b;
    }
    public EventDispatcher extraEvent;
    public EventDispatcher getExtraUIEvent(){
        if(this.extraEvent==null || this.extraEvent.closed){
            this.extraEvent=new EventDispatcher();
            this.extraEvent.setEventType(String.class);
        }
        return extraEvent;
    }
    long lastBackTime=0;
    long exitInterval=2000;
    public boolean _backPressed(){
        getExtraUIEvent().fireEvent("backPressed");
        if(!AndroidUIBase.interceptBackPressed){
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastBackTime < exitInterval) {
                return true;
            } else {
                lastBackTime = currentTime;
                String tips="Press again to exit";
                Configuration config = ApiServer.defaultAndroidContext.getResources().getConfiguration();
                Locale locale;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    locale = config.getLocales().get(0);
                } else {
                    locale = config.locale;
                }
                if(locale.getLanguage().equals("zh")){
                    tips="再按一次退出应用";
                }
                Toast.makeText(ApiServer.defaultAndroidContext, tips, Toast.LENGTH_SHORT).show();
                return false;
            }
        }else{
            return false;
        }

    }
}
