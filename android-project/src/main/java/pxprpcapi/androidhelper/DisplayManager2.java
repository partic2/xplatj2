package pxprpcapi.androidhelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import partic2.pxseedloader.android.launcher.ApiServer;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.BuiltInFuncList;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class DisplayManager2 implements Closeable {
    public static final String PxprpcNamespace="AndroidHelper.DisplayManager";
    public static DisplayManager2 i;
    public DisplayManager dm;
    public DisplayManager2(){
        dm= (DisplayManager) ApiServer.defaultAndroidContext.getSystemService(Context.DISPLAY_SERVICE);
        i=this;
    }
    public ByteBuffer listDisplayStaticConst() throws IllegalAccessException {
        return new BuiltInFuncList().listStaticConstField(Display.class);
    }
    public ByteBuffer getDevicesInfo(){
        Display[] displays = dm.getDisplays();
        TableSerializer ser = new TableSerializer().setColumnsInfo(null,new String[]{
                "id", "name", "width", "height","state","refreshRate","realWidth","realHeight","dpiX","dpiY"
        });
        for(Display d:displays){
            Point size=new Point();
            DisplayMetrics dmetrics=new DisplayMetrics();
            d.getSize(size);
            d.getRealMetrics(dmetrics);
            ser.addRow(new Object[]{
                    d.getDisplayId(),d.getName(),size.x,size.y,d.getState(),d.getRefreshRate(),
                    dmetrics.widthPixels,dmetrics.heightPixels,dmetrics.xdpi,dmetrics.ydpi,
            });
        }
        return ser.build();
    }
    @MethodTypeDecl("->ii")
    public Object[] getCurrentDisplaySize(){
        Display d=getDefaultDisplay();
        Point size=new Point();
        d.getSize(size);
        return new Object[]{size.x,size.y};
    }
    //Display ids are not guaranteed to be array indexes of getDisplays().
    private Display getDefaultDisplay(){
        WindowManager wm=(WindowManager)ApiServer.defaultAndroidContext.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay();
    }
    public int getCurrentDisplayDevice(){
        return getDefaultDisplay().getDisplayId();
    }

    public SurfaceManager.ImageOrBitmap viewReadPixels(AsyncReturn<SurfaceManager.ImageOrBitmap> ret,View v){
        new Handler(ApiServer.defaultAndroidContext.getMainLooper()).post(new Runnable(){
            @Override
            public void run() {
                try{
                    if(v==null || v.getWidth()<=0 || v.getHeight()<=0){
                        throw new IllegalStateException("View is not laid out yet");
                    }
                    SurfaceManager.ImageOrBitmap img = new SurfaceManager.ImageOrBitmap();
                    img.bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas cvs = new Canvas(img.bitmap);
                    v.draw(cvs);
                    ApiServer.resolveAsync(ret,img);
                }catch(Exception ex){
                    ApiServer.rejectAsync(ret,ex);
                }
            }
        });
        return null;
    }
    public View getCurrentMainView(){
        if(!(ApiServer.defaultAndroidContext instanceof Activity)){
            throw new IllegalStateException("defaultAndroidContext is not an Activity");
        }
        Activity at = (Activity) ApiServer.defaultAndroidContext;
        return at.getWindow().getDecorView();
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
    }
}
