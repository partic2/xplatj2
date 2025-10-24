package pxprpcapi.androidhelper;

import android.content.IntentFilter;
import partic2.pxseedloader.android.launcher.AssetsCopy;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.EventDispatcher;
import pxprpc.extend.TableSerializer;
import xplatj.javaplat.partic2.util.CloseableGroup;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class IntentReceiver extends PxprpcBroadcastReceiverAdapter implements Closeable {
    public static final String PxprpcNamespace="AndroidHelper.IntentReceiver";
    public static IntentReceiver i;

    @Override
    public void close() throws IOException {
        this.eventDispatcher().close();
    }

    public static class Evt{
        public String event;
        public long time;
        public ByteBuffer data;
    }
    public static class ECfg{
        public String event;
        public boolean startMain;
    }
    LinkedList<Evt> evts=new LinkedList<Evt>();
    HashMap<String,ECfg> ecfgs=new HashMap<String,ECfg>();
    public IntentReceiver(){
        i=this;
    }

    public void queueIntent(String event,ByteBuffer data){
        Evt t1=new Evt();
        t1.data=data;
        t1.event=event;
        t1.time=System.currentTimeMillis();
        this.evts.offer(t1);
        ECfg ecfg = this.ecfgs.get(event);
        if(ecfg!=null && ecfg.startMain){
            //Start Activity for
        }
        synchronized (this.waitingQueue){
            if(this.waitingQueue.size()>0){
                ByteBuffer ret = new TableSerializer().fromTypedObjectArray(this.evts).build();
                while(this.waitingQueue.size()>0){
                    this.waitingQueue.poll().resolve(ret);
                }
                this.evts.clear();
            }
        }
    }
    public LinkedList<AsyncReturn<ByteBuffer>> waitingQueue=new LinkedList<AsyncReturn<ByteBuffer>>();
    public ByteBuffer waitIntents(AsyncReturn<ByteBuffer> aret,int timeoutSec){
        if(this.evts.size()>0){
            aret.resolve(new TableSerializer().fromTypedObjectArray(this.evts).build());
        }else{
            synchronized (this.waitingQueue){
                while(this.waitingQueue.size()>20){
                    this.waitingQueue.poll().resolve(new TableSerializer().setColumnsInfo("",new String[0]).build());
                }
                waitingQueue.offer(aret);
            }
        }
        return null;
    }

    //To make binding generator happy. We need better way.
    public EventDispatcher eventDispatcher(){
        return super.eventDispatcher();
    }

    public void startListenEvent(String action){
        SysBase.i.getDefaultContext().registerReceiver(this,new IntentFilter(action));
    }

    public void stopListenEvent(String action){
        //TODO
    }

}
