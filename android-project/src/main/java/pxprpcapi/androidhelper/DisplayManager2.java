package pxprpcapi.androidhelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.view.Display;
import project.xplat.launcher.ApiServer;
import pxprpc.extend.BuiltInFuncList;
import pxprpc.extend.TableSerializer;

import java.nio.ByteBuffer;

public class DisplayManager2 {
    public static final String PxprpcNamespace="AndroidHelper.DisplayManager";
    public DisplayManager dm;
    public DisplayManager2(){
        dm= (DisplayManager) ApiServer.defaultAndroidContext.getSystemService(Context.DISPLAY_SERVICE);
    }
    public ByteBuffer listDisplayStaticConst() throws IllegalAccessException {
        return new BuiltInFuncList().listStaticConstField(Display.class);
    }
    public ByteBuffer getDevicesInfo(){
        Display[] displays = dm.getDisplays();
        TableSerializer ser = new TableSerializer().setHeader(null,new String[]{
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
    public int getCurrentDisplayDevice(){
        return ((Activity)ApiServer.defaultAndroidContext).getWindow().getWindowManager().getDefaultDisplay().getDisplayId();
    }
}
