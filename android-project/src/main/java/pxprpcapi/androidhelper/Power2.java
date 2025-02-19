package pxprpcapi.androidhelper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.PowerManager;
import project.xplat.launcher.ApiServer;
import pxprpc.extend.TableSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Power2 extends PxprpcBroadcastReceiverAdapter implements Closeable {

    public static final String PxprpcNamespace="AndroidHelper.Power";
    public static Power2 i;
    BatteryManager bm;
    PowerManager pm;

    public Power2(){
        pm = (PowerManager) ApiServer.defaultAndroidContext.getSystemService(Service.POWER_SERVICE);
        bm = (BatteryManager) ApiServer.defaultAndroidContext.getSystemService(Service.BATTERY_SERVICE);
        i=this;
    }

    public PowerManager.WakeLock cpuWake;
    public PowerManager.WakeLock screenWake;
    public void accuireCpuWakeLock(){
        if(cpuWake==null){
            cpuWake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"xplatj:ApiServer");
            cpuWake.acquire();
        }
    }
    public void accuireScreenWakeLock(boolean keepBright){
        if(screenWake==null){
            screenWake=pm.newWakeLock(keepBright?PowerManager.SCREEN_BRIGHT_WAKE_LOCK:PowerManager.SCREEN_DIM_WAKE_LOCK,"xplatj:ApiServer");
            screenWake.acquire();
        }
    }
    public void releaseWakeLock(){
        if(cpuWake!=null){
            cpuWake.release();
            cpuWake=null;
        }
        if(screenWake!=null){
            screenWake.release();
            screenWake=null;
        }
    }
    public ByteBuffer getBatteryState(){
        bm=(BatteryManager) ApiServer.defaultAndroidContext.getSystemService(Service.BATTERY_SERVICE);
        TableSerializer ser = new TableSerializer().setColumnInfo("iiii", new String[]{"chargeCounter", "currentAverage", "currentNow", "capacity"}).addRow(new Object[]{
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE),
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        });
        return ser.build();
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
    }
}
