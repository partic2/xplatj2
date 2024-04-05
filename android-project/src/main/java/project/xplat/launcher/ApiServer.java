package project.xplat.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import pxprpc.backend.TCPBackend;
import pxprpc.extend.DefaultFuncMap;
import pxprpcapi.androidhelper.*;
import pxprpcapi.jsehelper.JseIo;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.partic2.util.OneArgFunc;


import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.*;


public class ApiServer {
    public static TCPBackend tcpServ;
    public static Context defaultAndroidContext;
    public static HandlerThread handlerThread;
    public static Handler handler;
    public static int port=2050;
    public static IBinder serviceBinder;
    public static ServiceConnection serviceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ApiServer.serviceBinder=service;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            ApiServer.serviceBinder=null;
        }
    };

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
                putModule(SysBase.PxprpcNamespace,new SysBase());
                putModule(AndroidCamera2.PxprpcNamespace,new AndroidCamera2());
                putModule(Bluetooth2.PxprpcNamespace,new Bluetooth2());
                putModule(Intent2.PxprpcNamespace,new Intent2());
                putModule(Sensor2.PxprpcNamespace,new Sensor2());
                putModule(Wifi2.PxprpcNamespace,new Wifi2());
                putModule(Misc2.PxprpcNamespace,new Misc2());
                putModule(Power2.PxprpcNamespace,new Power2());
                putModule(SurfaceManager.PxprpcNamespace,new SurfaceManager());
                putModule(MediaProjection2.PxprpcNamespace,new MediaProjection2());
                putModule(JseIo.PxprpcNamespace,new JseIo());
                putModule(DisplayManager2.PxprpcNamespace,new DisplayManager2());
            }
        });
        if(!(ApiServer.defaultAndroidContext instanceof PxprpcService)){
            ApiServer.defaultAndroidContext.bindService(
                    new Intent(ApiServer.defaultAndroidContext,PxprpcService.class),
                    serviceConnection,Context.BIND_AUTO_CREATE);
        }
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
                if(ApiServer.serviceBinder!=null){
                    ApiServer.defaultAndroidContext.unbindService(ApiServer.serviceConnection);
                }
            }
        }).start();

    }
    //map<requestCode,callback:([requestCode,resultCode,data])->void>
    public static Map<Integer, OneArgFunc<Boolean,Object[]>> onActivityResultCallback =new HashMap<>();
    public static void onActivityResult(int requestCode, int resultCode, Intent data){
        if(onActivityResultCallback.containsKey(requestCode)){
            //we should prevent invoke callback on main thread.
            PlatCoreConfig.get().executor.execute(()->{
                onActivityResultCallback.get(requestCode).call(new Object[]{requestCode,resultCode,data});
            });
        }else{
            PlatCoreConfig.get().executor.execute(()->{
                onActivityResultCallback.get(0).call(new Object[]{requestCode,resultCode,data});
            });
        }
    }
}
