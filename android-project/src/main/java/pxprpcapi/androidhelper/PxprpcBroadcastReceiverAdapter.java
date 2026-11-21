package pxprpcapi.androidhelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import pxprpc.extend.EventDispatcher;
import xplatj.javaplat.partic2.util.PlatCoreConfig;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class PxprpcBroadcastReceiverAdapter extends BroadcastReceiver implements Closeable {
    //CopyOnWriteArraySet: dispatchers can be added/closed from RPC threads while
    //onReceive iterates them from the main thread.
    protected Set<EventDispatcher> dispatcher=new CopyOnWriteArraySet<EventDispatcher>();

    //targetSdk 34 requires an explicit export flag for non-system broadcasts.
    public static Intent registerReceiverCompat(Context c, BroadcastReceiver receiver, IntentFilter filter){
        if(Build.VERSION.SDK_INT>=34){
            return c.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);
        }else{
            return c.registerReceiver(receiver,filter);
        }
    }

    public final static int DUMP_TYPE_ACTION=1;
    public final static int DUMP_TYPE_EXTRA=2;
    //dumpType:
    //  "DUMP_TYPE_ACTION":only action is serialized as a string
    //  "DUMP_TYPE_EXTRA":action and extra bundle(if any) are serialized as a Table.
    protected int dumpType=DUMP_TYPE_EXTRA;
    public void setEventDumpType(int dumpType){
        this.dumpType=dumpType;
        if(dumpType==DUMP_TYPE_ACTION){
            for(EventDispatcher e:this.dispatcher){
                e.setEventType(String.class);
            }
        }else if(dumpType==DUMP_TYPE_EXTRA){
            for(EventDispatcher e:this.dispatcher){
                e.setEventType(ByteBuffer.class);
            }
        }
    }
    public PxprpcBroadcastReceiverAdapter(){}
    public EventDispatcher eventDispatcher(){
        EventDispatcher ep=new EventDispatcher();
        this.dispatcher.add(ep);
        this.setEventDumpType(this.dumpType);
        return ep;
    }

    protected Context autoUnregisterContexts=null;
    public EventDispatcher autoUnregisterEventDispatcher(Context c, String filter){
        if(this.autoUnregisterContexts==null){
            this.autoUnregisterContexts=c;
            registerReceiverCompat(c,this,new IntentFilter(filter));
            EventDispatcher ep=new EventDispatcher(){
                @Override
                public void close() throws IOException {
                    PxprpcBroadcastReceiverAdapter.this.close();
                    super.close();
                }
            };
            this.dispatcher.add(ep);
            this.setEventDumpType(this.dumpType);
            return ep;
        }else{
            return null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(this.dispatcher.isEmpty()){
            return;
        }
        if(this.dumpType==DUMP_TYPE_EXTRA){
            Bundle bundle=intent.getExtras();
            if(bundle==null){
                bundle=new Bundle();
            }
            bundle.putString("_intentAction",intent.getAction());
            final ByteBuffer result=SysBase.dumpBundle(bundle);
            PlatCoreConfig.get().executor.execute(new Runnable() {
                @Override
                public void run() {
                    for(EventDispatcher e:PxprpcBroadcastReceiverAdapter.this.dispatcher){
                        e.fireEvent(result);
                    }
                }
            });
        }else if(this.dumpType==DUMP_TYPE_ACTION){
            final String action=intent.getAction();
            PlatCoreConfig.get().executor.execute(new Runnable() {
                @Override
                public void run() {
                    for(EventDispatcher e:PxprpcBroadcastReceiverAdapter.this.dispatcher){
                        e.fireEvent(action);
                    }
                }
            });
        }

    }

    @Override
    public void close() throws IOException {
        //Reject pending calls before the receiver stops producing events.
        for(EventDispatcher e:this.dispatcher){
            try{
                e.close();
            }catch(Exception ex){}
        }
        this.dispatcher.clear();
        if(this.autoUnregisterContexts!=null){
            try{
                this.autoUnregisterContexts.unregisterReceiver(this);
            }catch(Exception ex){}
            this.autoUnregisterContexts=null;
        }
    }
}
