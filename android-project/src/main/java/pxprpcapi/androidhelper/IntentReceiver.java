package pxprpcapi.androidhelper;

import project.xplat.launcher.ApiServer;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.partic2.filesystem.IFile;
import xplatj.javaplat.partic2.filesystem.IFileSystem;
import xplatj.javaplat.partic2.io.IDataBlock;
import xplatj.javaplat.partic2.util.CloseableGroup;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class IntentReceiver implements Closeable {
    public static final String PxprpcNamespace="AndroidHelper.IntentReceiver";
    public static IntentReceiver i;
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
        init();
    }
    protected void init(){
        IFileSystem fs = PlatCoreConfig.get().fs;
        IFile cfgFile=fs.resolve("pxprpc/"+ IntentReceiver.PxprpcNamespace+"/ecfgs");
        if(cfgFile.exists()){
            CloseableGroup toClose=new CloseableGroup();
            try {
                IDataBlock data = cfgFile.open();
                toClose.add(()->data.free());
                ByteBuffer buf = ByteBuffer.allocate((int) data.size());
                for(ECfg it:new TableSerializer().load(buf).toTypedObjectArray(ECfg.class)){
                    ecfgs.put(it.event,it);
                }
            } catch (IOException e) {
            }finally {
                toClose.closeQuietly();
            }
        }
    }
    public void close() throws IOException {
        if(i==this)i=null;
        IFileSystem fs = PlatCoreConfig.get().fs;
        IFile cfgFile=fs.resolve("pxprpc/"+ IntentReceiver.PxprpcNamespace+"/ecfgs");
        if(!cfgFile.exists()) {
            cfgFile.create();
        }
        CloseableGroup toClose=new CloseableGroup();
        try {
            IDataBlock data = cfgFile.open();
            toClose.add(()->data.free());
            ArrayList<ECfg> lists = new ArrayList<ECfg>();
            lists.addAll(ecfgs.values());
            TableSerializer tab = new TableSerializer().fromTypedObjectArray(lists);
            ByteBuffer buf = tab.build();
            data.write(buf.array(),buf.position(),buf.remaining());
        } catch (IOException e) {
        }finally {
            toClose.closeQuietly();
        }
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
                    this.waitingQueue.poll().resolve(new TableSerializer().setHeader("",new String[0]).build());
                }
                waitingQueue.offer(aret);
            }
        }
        return null;
    }
}
