package project.xplat.android;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import project.xplat.launcher.ApiServer;
import project.xplat.launcher.AssetsCopy;
import project.xplat.launcher.PxprpcService;
import pxprpcapi.androidhelper.MediaProjection2;
import xplatj.gdxconfig.core.PlatCoreConfig;

public class BackgroundProcessService1 extends Service{
    public BackgroundProcessService1() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AssetsCopy.init(this);
        project.xplat.launcher.MainActivity.startOptsParsed[0]=false;
        ApiServer.port=ApiServer.port+1;
        ApiServer.defaultAndroidContext=this;
    }

    void bgThread() {
        ApiServer.start(this);
    }
    public boolean rpcsrvListening=false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!rpcsrvListening){
            rpcsrvListening=true;
            PlatCoreConfig.get().executor.execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            BackgroundProcessService1.this.bgThread();
                        }
                    }
            );
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public static class ServiceBinder extends Binder {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch(data.readInt()){
                case MediaProjection2.ServiceBinderCode:
                    return MediaProjection2.i.mediaProjectionRequest(data,reply);
            }
            return true;
        }
    }
    @Override
    public void onDestroy() {
        ApiServer.stop();
        super.onDestroy();
    }
    public PxprpcService.ServiceBinder mBinder;
    @Override
    public IBinder onBind(Intent intent) {
        this.mBinder=new PxprpcService.ServiceBinder();
        return this.mBinder;
    }
}
