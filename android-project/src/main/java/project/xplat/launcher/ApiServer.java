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
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.DefaultFuncMap;
import pxprpc.extend.EventDispatcher;
import pxprpcapi.androidhelper.*;
import pxprpcapi.jsehelper.JseIo;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.partic2.util.OneArgFunc;


import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        //Put init into handlerThread to avoid Looper error.
        handler.post(new Runnable(){
            @Override
            public void run() {
                if(SysBase.i==null){
                    new SysBase();
                }
                if(AndroidCamera2.i==null){
                    new AndroidCamera2();
                }
                if(Bluetooth2.i==null){
                    new Bluetooth2();
                }
                if(Intent2.i==null){
                    new Intent2();
                }
                if(IntentReceiver.i==null){
                    new IntentReceiver();
                }
                if(Sensor2.i==null){
                    new Sensor2();
                }
                if(Wifi2.i==null){
                    new Wifi2();
                }
                if(Misc2.i==null){
                    new Misc2();
                }
                if(Power2.i==null) {
                    new Power2();
                }
                if(SurfaceManager.i==null){
                    new SurfaceManager();
                }
                if(MediaProjection2.i==null){
                    new MediaProjection2();
                }
                if(DisplayManager2.i==null){
                    new DisplayManager2();
                }
                if(AndroidUIBase.i==null){
                    new AndroidUIBase();
                }
                if(JseIo.i==null){
                    new JseIo();
                }
                putModule(SysBase.PxprpcNamespace,SysBase.i);
                putModule(AndroidCamera2.PxprpcNamespace,AndroidCamera2.i);
                putModule(Bluetooth2.PxprpcNamespace,Bluetooth2.i);
                putModule(Intent2.PxprpcNamespace,Intent2.i);
                putModule(Sensor2.PxprpcNamespace,Sensor2.i);
                putModule(Wifi2.PxprpcNamespace,Wifi2.i);
                putModule(Misc2.PxprpcNamespace,Misc2.i);
                putModule(Power2.PxprpcNamespace,Power2.i);
                putModule(SurfaceManager.PxprpcNamespace,SurfaceManager.i);
                putModule(MediaProjection2.PxprpcNamespace,MediaProjection2.i);
                putModule(DisplayManager2.PxprpcNamespace,DisplayManager2.i);
                putModule(AndroidUIBase.PxprpcNamespace,AndroidUIBase.i);
                putModule(JseIo.PxprpcNamespace,JseIo.i);
                putModule(IntentReceiver.PxprpcNamespace,IntentReceiver.i);
                try {
                    Class<?> extendMain = Class.forName("pxprpcapi.androidhelperex.Main");
                    Method register = extendMain.getMethod("registerPxprpcApi");
                    register.invoke(null);
                } catch (Exception e) {
                }
            }
        });
        if(!(ApiServer.defaultAndroidContext instanceof PxprpcService)){
            ApiServer.defaultAndroidContext.bindService(
                    new Intent(ApiServer.defaultAndroidContext,PxprpcService.class),
                    serviceConnection,Context.BIND_AUTO_CREATE);
        }
        Log.d("PxpRpc", "start: listen");
        for(;port<20000;port+=2048){
            try{
                if(PlatCoreConfig.debugMode){
                    tcpServ.bindAddr= new InetSocketAddress(
                            Inet4Address.getByAddress(new byte[]{(byte)0,(byte)0,(byte)0,(byte)0}),port);
                }else{
                    tcpServ.bindAddr= new InetSocketAddress(
                            Inet4Address.getByAddress(new byte[]{(byte)127,(byte)0,(byte)0,(byte)1}),port);
                }
                tcpServ.listenAndServe();
                break;
            }catch (IOException ex){
            }
        }
        if(port>=20000){
            throw new RuntimeException("No available tcp port.");
        }
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
                try{
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
                }catch(Exception ex){
                    //mute deinit error for runtime.
                    ex.printStackTrace();
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

    public static <T> void resolveAsync(AsyncReturn<T> aret, final T result){
        PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                aret.resolve(result);
            }
        });
    }
    public static void rejectAsync(AsyncReturn<?> aret, final Exception error){
        PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                aret.reject(error);
            }
        });
    }
    public static void fireEventAsync(EventDispatcher dispatcher,final Object event){
        PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                dispatcher.fireEvent(event);
            }
        });
    }

}
