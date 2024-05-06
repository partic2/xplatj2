package pxprpcapi.androidhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import project.xplat.launcher.ApiServer;
import pxprpc.base.Serializer2;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.EventDispatcher;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class AndroidUIBase implements Closeable {
    public static final String PxprpcNamespace="AndroidHelper.AndroidUIBase";
    public static AndroidUIBase i;
    public Handler mainTaskQueue;
    public AndroidUIBase(){
        i=this;
        mainTaskQueue=new Handler(ApiServer.defaultAndroidContext.getMainLooper());
    }
    public void dispatchKeyEvent(int action,int code){
        mainTaskQueue.post(()-> {
            Activity activity = (Activity) ApiServer.defaultAndroidContext;
            KeyEvent keyEvent = new KeyEvent(action, code);
            activity.dispatchKeyEvent(keyEvent);
        });
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
    }

    public static class TouchPointers{
        MotionEvent.PointerProperties[] props;
        MotionEvent.PointerCoords[] coords;
    }
    private <T> int indexOf(T[] arr,T val){
        for(int i=0;i<arr.length;i++){
            if(val.equals(arr[i])){
                return i;
            }
        }
        return -1;
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
            r.props[i].id=(int)row[0];
            r.coords[i]=new MotionEvent.PointerCoords();
            r.coords[i].x=(int)row[1];
            r.coords[i].y=(int)row[2];
            if(row.length>3){
                r.coords[i].pressure=(int)row[3];
            }
        }
        return r;
    }
    public void dispatchTouchEvent(int action,TouchPointers touchPointers){
        mainTaskQueue.post(()->{
            Activity activity = (Activity) ApiServer.defaultAndroidContext;
            MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),
                    action,touchPointers.coords.length,touchPointers.props,touchPointers.coords,
                    0,0,1.0f,1.0f,0,0,0,0);
        });
    }

    public void webViewSetStartScript(String script){
        project.xplat.webapp.MainActivity activity=(project.xplat.webapp.MainActivity)ApiServer.defaultAndroidContext;
        activity.setWebviewStartScript(script);
    }
    public void webViewRunJs(String script){
        mainTaskQueue.post(()-> {
            project.xplat.webapp.MainActivity activity=(project.xplat.webapp.MainActivity)ApiServer.defaultAndroidContext;
            activity.webviewRunJs(script);
        });
    }

    public View getCurrentMainContent(){
        Activity activity = (Activity) ApiServer.defaultAndroidContext;
        return ((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
    }
    EventDispatcher dialogEvent=new EventDispatcher().setEventType(String.class);
    public EventDispatcher dialogEvent(){
        return dialogEvent;
    }
    public void dialogSet(AlertDialog dialog,String msg,boolean show){
        mainTaskQueue.post(()->{
            dialog.setMessage(msg);
            if(show && !dialog.isShowing()){
                dialog.show();
            }else if(!show && dialog.isShowing()){
                dialog.hide();
            }
        });
    }
    @MethodTypeDecl("o->c")
    public Object[] dialogGet(AlertDialog dialog){
        return new Object[]{dialog.isShowing()};
    }
    public void dialogNew(AsyncReturn<AlertDialog> aret,String btn1,String id1,String btn2,String id2){
        mainTaskQueue.post(()-> {
            Activity activity = (Activity) ApiServer.defaultAndroidContext;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("");
            if(!btn1.equals("")){
                builder.setPositiveButton(btn1,(dialog, which)->{
                    dialogEvent.fireEvent(id1);
                });
            }
            if(!btn2.equals("")){
                builder.setNegativeButton(btn2,(dialog, which)->{
                    dialogEvent.fireEvent(id2);
                });
            }
            aret.resolve(builder.create());
        });
    }


}
