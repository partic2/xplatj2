package pxprpcapi.androidhelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import project.xplat.launcher.ApiServer;
import pxprpc.extend.EventDispatcher;
import xplatj.gdxconfig.core.PlatCoreConfig;

public class PxprpcBroadcastReceiverAdapter extends BroadcastReceiver {

    protected EventDispatcher dispatcher;

    public PxprpcBroadcastReceiverAdapter(){
        this.dispatcher=new EventDispatcher();
    }

    protected boolean started=false;

    public EventDispatcher eventDispatcher(){
        started=true;
        return dispatcher;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(started){
            Bundle bundle = intent.getExtras();
            bundle.putString("_intentAction",intent.getAction());
            PlatCoreConfig.get().executor.execute(new Runnable() {
                @Override
                public void run() {
                    PxprpcBroadcastReceiverAdapter.this.dispatcher.fireEvent(SysBase.i.dumpBundle(bundle));
                }
            });
        }
    }
}
