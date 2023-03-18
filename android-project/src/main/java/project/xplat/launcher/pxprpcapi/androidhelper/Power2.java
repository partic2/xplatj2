package project.xplat.launcher.pxprpcapi.androidhelper;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import project.xplat.launcher.pxprpcapi.ApiServer;
import project.xplat.launcher.pxprpcapi.Utils;
import pursuer.patchedmsgpack.tools.MapBuilder2;
import pursuer.pxprpc.EventDispatcher;

public class Power2 extends PxprpcBroadcastReceiverAdapter {

    public static final String PxprpcNamespace="AndroidHelper-Power";


    BatteryManager bm;
    PowerManager pm;

    public Power2(){
        pm = (PowerManager) ApiServer.defaultAndroidContext.getSystemService(Service.POWER_SERVICE);
        bm = (BatteryManager) ApiServer.defaultAndroidContext.getSystemService(Service.BATTERY_SERVICE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.eventDispatcher().fireEvent(intent.getAction());
    }

    public PowerManager.WakeLock cpuWake;
    public PowerManager.WakeLock screenWake;
    public void accuireCpuWakeLock(){
        if(cpuWake!=null){
            cpuWake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"xplatj:ApiServer");
        }
    }
    public void accuireScreenWakeLock(boolean keepBright){
        if(screenWake!=null){
            screenWake=pm.newWakeLock(keepBright?PowerManager.SCREEN_BRIGHT_WAKE_LOCK:PowerManager.SCREEN_DIM_WAKE_LOCK,"xplatj:ApiServer");
        }
    }
    public void releaseWakeLock(){
        if(cpuWake!=null){
            cpuWake.release();
        }
        if(screenWake!=null){
            screenWake.release();
        }
    }
    public byte[] getBatteryState(){
        bm=(BatteryManager) ApiServer.defaultAndroidContext.getSystemService(Service.BATTERY_SERVICE);
        return Utils.packFrom(new MapBuilder2()
                .put("chargeCounter",bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))
                .put("currentAverage",bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE))
                .put("currentNow",bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
                .put("capacity",bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
                .build());
    }
}
