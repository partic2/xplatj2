package project.xplat.launcher;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import pxprpcapi.androidhelper.MediaProjection2;
import xplatj.gdxconfig.core.PlatCoreConfig;

public class PxprpcService extends Service {
    public PxprpcService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AssetsCopy.init(this);
        project.xplat.launcher.MainActivity.startOptsParsed[0]=false;
        ApiServer.port=ApiServer.port+1;
        ApiServer.portRange[0]++;
        ApiServer.portRange[1]++;
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
                            PxprpcService.this.bgThread();
                        }
                    }
            );
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public static class ServiceBinder extends Binder{
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
    public ServiceBinder mBinder;
    @Override
    public IBinder onBind(Intent intent) {
        this.mBinder=new ServiceBinder();
        return this.mBinder;
    }
}