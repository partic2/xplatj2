package project.xplat.webapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import project.xplat.launcher.ApiServer;
import project.xplat.launcher.AssetsCopy;
import pxprpcapi.androidhelper.MediaProjection2;
import xplatj.gdxconfig.core.PlatCoreConfig;

public class AndroidNodeProcess extends Service {
    public AndroidNodeProcess() {
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
    public project.xplat.launcher.PxprpcService.ServiceBinder mBinder;
    @Override
    public IBinder onBind(Intent intent) {
        this.mBinder=new project.xplat.launcher.PxprpcService.ServiceBinder();
        return this.mBinder;
    }
}