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
import project.xplat.launcher.ApiServer;
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
        TableSerializer ser = new TableSerializer().setColumnInfo(null,new String[]{
                "id", "name", "width", "height","state","refreshRate","realWidth","realHeight","dpiX","dpiY"
        });
        for(Display d:displays){
            Point size=new Point();
            DisplayMetrics dm=new DisplayMetrics();
            d.getSize(size);
            d.getRealMetrics(dm);
            ser.addRow(new Object[]{
                    d.getDisplayId(),d.getName(),size.x,size.y,d.getState(),d.getRefreshRate(),
                    dm.widthPixels,dm.heightPixels,dm.xdpi,dm.ydpi,
            });
            d.getName();
        }
        return ser.build();
    }
    @MethodTypeDecl("->ii")
    public Object[] getCurrentDisplaySize(){
        Display dm2 = dm.getDisplays()[getCurrentDisplayDevice()];
        Point size=new Point();
        dm2.getSize(size);
        return new Object[]{size.x,size.y};
    }
    public int getCurrentDisplayDevice(){
        return ((Activity)ApiServer.defaultAndroidContext).getWindow().getWindowManager().getDefaultDisplay().getDisplayId();
    }

    public SurfaceManager.ImageOrBitmap viewReadPixels(AsyncReturn<SurfaceManager.ImageOrBitmap> ret,View v){
        new Handler(ApiServer.defaultAndroidContext.getMainLooper()).post(new Runnable(){
            @Override
            public void run() {
                SurfaceManager.ImageOrBitmap img = new SurfaceManager.ImageOrBitmap();
                img.bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas cvs = new Canvas(img.bitmap);
                v.draw(cvs);
                ret.resolve(img);
            }
        });
        return null;
    }
    public View getCurrentMainView(){
        Activity at = (Activity) ApiServer.defaultAndroidContext;
        return at.getWindow().getDecorView();
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
    }
}
