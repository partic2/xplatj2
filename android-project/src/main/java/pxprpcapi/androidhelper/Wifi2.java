package pxprpcapi.androidhelper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import partic2.pxseedloader.android.launcher.ApiServer;
import partic2.pxseedloader.android.launcher.PxprpcService;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.TableSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Wifi2 extends PxprpcBroadcastReceiverAdapter implements Closeable {
    public static final String PxprpcNamespace="AndroidHelper.Wifi";
    public static Wifi2 i;
    WifiManager wm;
    WifiP2pManager wpm;
    ConnectivityManager cm;
    protected Context registeredContext;
    public Wifi2(){
        wm=(WifiManager) ApiServer.defaultAndroidContext.getSystemService(Context.WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            wpm=(WifiP2pManager) ApiServer.defaultAndroidContext.getSystemService(Context.WIFI_P2P_SERVICE);
        }
        cm=(ConnectivityManager) ApiServer.defaultAndroidContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        IntentFilter if2 = new IntentFilter();
        if2.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        if2.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        if2.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        if2.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        if2.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        if2.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        Context ctx=PxprpcService.current!=null?PxprpcService.current:ApiServer.defaultAndroidContext;
        try{
            registerReceiverCompat(ctx,this,if2);
            registeredContext=ctx;
        }catch(Exception ex){
            ex.printStackTrace();
        }
        i=this;
        this.setEventDumpType(DUMP_TYPE_ACTION);
    }
    public void close(){
        if(i==this)i=null;
        try {
            if(registeredContext!=null){
                registeredContext.unregisterReceiver(this);
                registeredContext=null;
            }
        }catch(Exception e){}
        try{
            super.close();
        }catch(Exception e){}
    }
    protected WifiManager requireWifiManager(){
        if(wm==null){
            throw new IllegalStateException("WifiManager is not available");
        }
        return wm;
    }
    public void scan(){
        requireWifiManager().startScan();
    }
    public List<ScanResult> getScanResult(){
        return requireWifiManager().getScanResults();
    }
    public ByteBuffer getScanResultInfo(){
        return packScanResult(requireWifiManager().getScanResults());
    }
    public ByteBuffer packScanResult(List<ScanResult> l){
        TableSerializer ser = new TableSerializer().setColumnsInfo(null, new String[]{
                "SSID", "level", "frequency", "capabilities"});
        for(ScanResult r:l){
        	ser.addRow(new Object[]{r.SSID,r.level,r.frequency,r.capabilities});
        }
        return ser.build();
    }
    public ByteBuffer getWifiInfo1(){
        WifiManager w=requireWifiManager();
        return new TableSerializer().setColumnsInfo(null,new String[]{
                "5GHzBandSupported","P2pSupported"
                }).addRow(new Object[]{
                w.is5GHzBandSupported(),w.isP2pSupported()
        }).build();
    }
    public ByteBuffer getState(){
        WifiManager w=requireWifiManager();
        return new TableSerializer().setColumnsInfo(null,new String[]{
                "WifiEnabled","WifiState"
        }).addRow(new Object[]{
                w.isWifiEnabled(),w.getWifiState()
        }).build();
    }
    public void setWifiEnable(boolean enable){
        requireWifiManager().setWifiEnabled(enable);
    }
    public void disconnect(){
        requireWifiManager().disconnect();
    }

    public void connectTo(String ssid,String psk) throws IOException {
        WifiManager w=requireWifiManager();
        WifiConfiguration conf=new WifiConfiguration();
        conf.SSID=quote(ssid);
        conf.hiddenSSID=false;
        conf.status=WifiConfiguration.Status.ENABLED;
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        if(psk==null || psk.length()==0){
            //Open network: WPA key management would make the config invalid.
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }else{
            conf.preSharedKey=quote(psk);
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        }
        int netid=w.addNetwork(conf);
        if(netid==-1){
            throw new IOException("addNetwork failed, check WIFI permissions or existing configuration for "+ssid);
        }
        if(!w.enableNetwork(netid,true)){
            throw new IOException("enableNetwork failed for netid "+netid);
        }
    }
    protected static String quote(String s){
        if(s==null){
            return "\"\"";
        }
        return (s.startsWith("\"") && s.endsWith("\""))?s:"\""+s+"\"";
    }
    Method fnSetWifiApEnabled;
    protected WifiConfiguration lastApConf;
    public void startWifiAp(String ssid,String psk) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if(fnSetWifiApEnabled==null)
            fnSetWifiApEnabled=wm.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID=quote(ssid);
        if(psk==null || psk.length()==0){
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }else{
            conf.preSharedKey=quote(psk);
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        }
        conf.hiddenSSID=false;
        lastApConf=conf;
        fnSetWifiApEnabled.invoke(wm, conf, true);

    }
    public void stopWifiAp() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if(lastApConf==null){
            return;
        }
        if(fnSetWifiApEnabled==null)
            fnSetWifiApEnabled=wm.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
        fnSetWifiApEnabled.invoke(wm,lastApConf,false);
        lastApConf=null;
    }

    protected WifiP2pManager.Channel defaultChannel;
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public WifiP2pManager.Channel p2pInit(){
        if(wpm==null){
            throw new UnsupportedOperationException("Wi-Fi P2P is not supported on this device");
        }
        this.defaultChannel=wpm.initialize(ApiServer.defaultAndroidContext,ApiServer.handlerThread.getLooper(), new WifiP2pManager.ChannelListener(){
            @Override
            public void onChannelDisconnected() {
                defaultChannel=null;
            }
        });
        if(this.defaultChannel==null){
            throw new IllegalStateException("Failed to initialize Wi-Fi P2P channel");
        }
        return this.defaultChannel;
    }
    protected WifiP2pManager.Channel requireP2pChannel(AsyncReturn<?> aret){
        if(wpm==null){
            aret.reject(new UnsupportedOperationException("Wi-Fi P2P is not supported on this device"));
            return null;
        }
        if(defaultChannel==null){
            aret.reject(new IllegalStateException("Wi-Fi P2P is not initialized, call p2pInit first"));
            return null;
        }
        return defaultChannel;
    }
    @SuppressLint("MissingPermission")
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void p2pStartDiscover(final AsyncReturn<Exception> aret){
        final WifiP2pManager.Channel ch=requireP2pChannel(aret);
        if(ch==null){
            return;
        }
        wpm.discoverPeers(ch,new WifiP2pManager.ActionListener(){
            @Override
            public void onSuccess() {
                aret.resolve(null);
            }
            @Override
            public void onFailure(int reason) {
                aret.reject(new IOException("wifip2p manager return  reason:"+reason));
            }
        });
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void p2pStopDiscover(final AsyncReturn<Exception> aret){
        final WifiP2pManager.Channel ch=requireP2pChannel(aret);
        if(ch==null){
            return;
        }
        wpm.stopPeerDiscovery(ch, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                aret.resolve(null);
            }

            @Override
            public void onFailure(int reason) {
                aret.reject(new IOException("wifip2p manager return  reason:"+reason));
            }
        });
    }
    @SuppressLint("MissingPermission")
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    //return ArrayList<WifiP2pDevice>
    public Object p2pGetPeersList(final AsyncReturn<ArrayList<WifiP2pDevice>> aret){
        final WifiP2pManager.Channel ch=requireP2pChannel(aret);
        if(ch==null){
            return null;
        }
        wpm.requestPeers(ch, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                ArrayList<WifiP2pDevice> peers=new ArrayList<WifiP2pDevice>();
                peers.addAll(peerList.getDeviceList());
                aret.resolve(peers);
            }
        });
        return null;
    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public ByteBuffer describeP2pPeersInfo(ArrayList<WifiP2pDevice> peers){
        TableSerializer ser = new TableSerializer().setColumnsInfo(null, new String[]{"deviceAddress", "deviceName", "status"});
        for(WifiP2pDevice p:peers){
            ser.addRow(new Object[]{p.deviceAddress,p.deviceName,p.status});
        }
        return ser.build();
    }

    @SuppressLint("MissingPermission")
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void p2pConnect(final AsyncReturn<Exception> aret,String addr){
        final WifiP2pManager.Channel ch=requireP2pChannel(aret);
        if(ch==null){
            return;
        }
        WifiP2pConfig conf = new WifiP2pConfig();
        conf.deviceAddress=addr;
        wpm.connect(ch,conf, new WifiP2pManager.ActionListener(){
            @Override
            public void onSuccess() {
                aret.resolve(null);
            }
            @Override
            public void onFailure(int reason) {
                aret.reject(new IOException("wifip2p manager return  reason:"+reason));
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void p2pCancelConnect(final AsyncReturn<Exception> aret){
        final WifiP2pManager.Channel ch=requireP2pChannel(aret);
        if(ch==null){
            return;
        }
        wpm.cancelConnect(ch, new WifiP2pManager.ActionListener(){
            @Override
            public void onSuccess() {
                aret.resolve(null);
            }
            @Override
            public void onFailure(int reason) {
                aret.reject(new IOException("wifip2p manager return  reason:"+reason));
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void p2pDisconnect(final AsyncReturn<Exception> aret) {
        final WifiP2pManager.Channel ch=requireP2pChannel(aret);
        if(ch==null){
            return;
        }
        wpm.removeGroup(ch, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                aret.resolve(null);
            }

            @Override
            public void onFailure(int reason) {
                aret.reject(new IOException("wifip2p manager return  reason:" + reason));
            }
        });
    }

}
