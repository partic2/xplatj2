package partic2.pxseedloader.android.launcher;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import pxprpcapi.androidhelper.MediaProjection2;
import xplatj.javaplat.partic2.util.PlatCoreConfig;

public class PxprpcService extends Service {
    public PxprpcService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    public static volatile PxprpcService current;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(current==null){
            current=this;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public static class ServiceBinder extends Binder{
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            return true;
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    public ServiceBinder mBinder;
    @Override
    public IBinder onBind(Intent intent) {
        this.mBinder=new ServiceBinder();
        return this.mBinder;
    }
}