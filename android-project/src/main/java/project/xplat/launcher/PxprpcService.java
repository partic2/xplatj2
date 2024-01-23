package project.xplat.launcher;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import xplatj.gdxconfig.core.PlatCoreConfig;

public class PxprpcService extends Service {
    public PxprpcService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ApiServer.port=ApiServer.port+1;
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

    @Override
    public void onDestroy() {
        ApiServer.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}