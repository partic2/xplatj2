package project.xplat.launcher.pxprpcapi.androidhelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import pursuer.pxprpc.EventDispatcher;

public class PxprpcBroadcastReceiverAdapter extends BroadcastReceiver {

    protected EventDispatcher dispatcher;

    public PxprpcBroadcastReceiverAdapter(){
        this.dispatcher=new EventDispatcher();
    }

    public EventDispatcher eventDispatcher(){
        return dispatcher;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.dispatcher.fireEvent(intent.getAction());
    }
}
