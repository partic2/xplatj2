package project.xplat.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import pxprpc.backend.TCPBackend;
import pxprpc.extend.DefaultFuncMap;
import pxprpcapi.androidhelper.*;
import pxprpcapi.jsehelper.JseIo;


import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;


public class ApiServer {
    public static TCPBackend tcpServ;
    public static Context defaultAndroidContext;
    public static HandlerThread handlerThread;
    public static Handler handler;
    public static int port=2050;
    
    public static SysBase sysbase;
    public static AndroidCamera2 androidcamera2;

    public static Handler getHandler(){
        return handler;
    }
    public static void serve() throws IOException {
        tcpServ = new TCPBackend();
        handlerThread = new HandlerThread("PxpRpcHandlerThread");
        handlerThread.start();
        while(handlerThread.getLooper()==null){}
        handler=new Handler(handlerThread.getLooper());
        MainActivity.ensureStartOpts();
        if(MainActivity.debugMode){
            tcpServ.bindAddr= new InetSocketAddress(
                    Inet4Address.getByAddress(new byte[]{(byte)0,(byte)0,(byte)0,(byte)0}),port);
        }else{
            tcpServ.bindAddr= new InetSocketAddress(
                    Inet4Address.getByAddress(new byte[]{(byte)127,(byte)0,(byte)0,(byte)1}),port);
        }
        //Put init into handlerThread to avoid Looper error.
        handler.post(new Runnable(){
            @Override
            public void run() {
            	ApiServer.sysbase=new SysBase();
            	ApiServer.androidcamera2=new AndroidCamera2();
                putModule(SysBase.PxprpcNamespace,sysbase);
                putModule(AndroidCamera2.PxprpcNamespace,androidcamera2);
                putModule(Bluetooth2.PxprpcNamespace,new Bluetooth2());
                putModule(Intent2.PxprpcNamespace,new Intent2());
                putModule(Sensor2.PxprpcNamespace,new Sensor2());
                putModule(Wifi2.PxprpcNamespace,new Wifi2());
                putModule(Misc2.PxprpcNamespace,new Misc2());
                putModule(Power2.PxprpcNamespace,new Power2());
                putModule(JseIo.PxprpcNamespace,new JseIo());
            }
        });
        Log.d("PxpRpc", "start: listen");
        tcpServ.listenAndServe();
    }
    public static void putModule(String modName,Object module){
        DefaultFuncMap.registered.put(modName,module);
    }
    public static Object getModule(String modName){
        return tcpServ.funcMap.get(modName);
    }

    public static void start(Context context) {
        defaultAndroidContext=context;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ApiServer.serve();
                } catch (IOException e) {
                }
            }
        }).start();
    }
    public static void closeQuietly(Closeable c){
        try {
            c.close();
        } catch (IOException e) {
        }
    }
    public static void stop(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                closeQuietly(tcpServ);
                for(Object mod:DefaultFuncMap.registered.values()){
                    if(mod instanceof Closeable){
                        closeQuietly((Closeable) mod);
                    }
                }
                tcpServ=null;
            }
        }).start();

    }
}