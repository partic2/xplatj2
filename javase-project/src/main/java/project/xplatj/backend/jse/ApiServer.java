package project.xplatj.backend.jse;



import project.xplatj.JMain;
import pxprpc.backend.TCPBackend;
import pxprpc.extend.DefaultFuncMap;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.platform.PlatApi;
import xplatj.pxprpcapi.JseBaseOsHelper;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;


public class ApiServer {
    public static TCPBackend tcpServ;
    public static int port=2050;

    public static void serve() throws IOException {
        tcpServ = new TCPBackend();
        if(JMain.debugMode){
            tcpServ.bindAddr= new InetSocketAddress(
                    Inet4Address.getByAddress(new byte[]{(byte)0,(byte)0,(byte)0,(byte)0}),port);
        }else{
            tcpServ.bindAddr= new InetSocketAddress(
                    Inet4Address.getByAddress(new byte[]{(byte)127,(byte)0,(byte)0,(byte)1}),port);
        }
        putModule(JseBaseOsHelper.PxprpcNamespace,new JseBaseOsHelper());
        tcpServ.listenAndServe();
    }
    public static void putModule(String modName,Object module){
        DefaultFuncMap.registered.put(modName,module);
    }
    public static Object getModule(String modName){
        return tcpServ.funcMap.get(modName);
    }

    public static void start() {
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
                for(Object mod: DefaultFuncMap.registered.values()){
                    if(mod instanceof Closeable){
                        closeQuietly((Closeable) mod);
                    }
                }
                tcpServ=null;
            }
        }).start();

    }
}
