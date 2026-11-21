package pxprpcapi.androidhelper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import partic2.pxseedloader.android.launcher.ApiServer;
import partic2.pxseedloader.android.launcher.PxprpcService;
import pxprpc.extend.TableSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Power2 extends PxprpcBroadcastReceiverAdapter implements Closeable {

    public static final String PxprpcNamespace="AndroidHelper.Power";
    public static Power2 i;
    BatteryManager bm;
    PowerManager pm;
    protected Context registeredContext;

    public Power2(){
        pm = (PowerManager) ApiServer.defaultAndroidContext.getSystemService(Service.POWER_SERVICE);
        bm = (BatteryManager) ApiServer.defaultAndroidContext.getSystemService(Service.BATTERY_SERVICE);
        IntentFilter filter=new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        Context ctx=PxprpcService.current!=null?PxprpcService.current:ApiServer.defaultAndroidContext;
        try{
            registerReceiverCompat(ctx,this,filter);
            registeredContext=ctx;
        }catch(Exception ex){
            //No context can host the receiver; the query style calls still work.
        }
        i=this;
        //Battery broadcasts are much more useful with their extras (level etc.).
        this.setEventDumpType(DUMP_TYPE_EXTRA);
    }

    public PowerManager.WakeLock cpuWake;
    public PowerManager.WakeLock screenWake;
    public synchronized void accuireCpuWakeLock(){
        if(cpuWake==null){
            cpuWake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"pxseed-loader:ApiServer");
            cpuWake.setReferenceCounted(false);
        }
        if(!cpuWake.isHeld()){
            cpuWake.acquire();
        }
    }
    public synchronized void accuireScreenWakeLock(boolean keepBright){
        if(screenWake==null){
            screenWake=pm.newWakeLock(keepBright?PowerManager.SCREEN_BRIGHT_WAKE_LOCK:PowerManager.SCREEN_DIM_WAKE_LOCK,"pxseedloader:ApiServer");
            screenWake.setReferenceCounted(false);
        }
        if(!screenWake.isHeld()){
            screenWake.acquire();
        }
    }
    public synchronized void releaseWakeLock(){
        //Releasing a lock that is not held throws RuntimeException.
        if(cpuWake!=null){
            try{
                if(cpuWake.isHeld())cpuWake.release();
            }catch(Exception ignored){}
            cpuWake=null;
        }
        if(screenWake!=null){
            try{
                if(screenWake.isHeld())screenWake.release();
            }catch(Exception ignored){}
            screenWake=null;
        }
    }
    public ByteBuffer getBatteryState(){
        bm=(BatteryManager) ApiServer.defaultAndroidContext.getSystemService(Service.BATTERY_SERVICE);
        TableSerializer ser = new TableSerializer().setColumnsInfo("iiii", new String[]{"chargeCounter", "currentAverage", "currentNow", "capacity"}).addRow(new Object[]{
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE),
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        });
        return ser.build();
    }
    public ByteBuffer describeBattery(){
        Context ctx=registeredContext!=null?registeredContext:ApiServer.defaultAndroidContext;
        Intent sticky=ctx.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        TableSerializer ser = new TableSerializer().setColumnsInfo("iiiiiiici", new String[]{
                "level","scale","status","plugged","health","temperature","voltage","present","percentage"});
        if(sticky!=null){
            int level=sticky.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            int scale=sticky.getIntExtra(BatteryManager.EXTRA_SCALE,100);
            int percentage=scale>0?level*100/scale:0;
            ser.addRow(new Object[]{
                    level,
                    scale,
                    sticky.getIntExtra(BatteryManager.EXTRA_STATUS,BatteryManager.BATTERY_STATUS_UNKNOWN),
                    sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED,0),
                    sticky.getIntExtra(BatteryManager.EXTRA_HEALTH,BatteryManager.BATTERY_HEALTH_UNKNOWN),
                    sticky.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0),
                    sticky.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0),
                    sticky.getBooleanExtra(BatteryManager.EXTRA_PRESENT,false),
                    percentage
            });
        }
        return ser.build();
    }
    public boolean isInteractive(){
        return pm.isInteractive();
    }
    public boolean isPowerSaveMode(){
        return pm.isPowerSaveMode();
    }
    public boolean isDeviceIdleMode(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            return pm.isDeviceIdleMode();
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        releaseWakeLock();
        try{
            super.close();
        }catch(Exception ex){}
        if(i==this)i=null;
    }
}
