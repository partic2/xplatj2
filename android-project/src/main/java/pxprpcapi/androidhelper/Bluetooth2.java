package pxprpcapi.androidhelper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import partic2.pxseedloader.android.launcher.ApiServer;
import partic2.pxseedloader.android.launcher.PxprpcService;
import pxprpc.base.Serializer2;
import pxprpc.base.Utils;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.BuiltInFuncList;
import pxprpc.extend.TableSerializer;
import xplatj.javaplat.partic2.util.PlatCoreConfig;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Bluetooth2 extends PxprpcBroadcastReceiverAdapter implements BluetoothAdapter.LeScanCallback,Closeable {
    public static final String PxprpcNamespace="AndroidHelper.Bluetooth";
    public static Bluetooth2 i;
    public BluetoothAdapter adapter;
    protected Context registeredContext;
    public Bluetooth2(){
        initDefault();
        i=this;
        this.setEventDumpType(DUMP_TYPE_ACTION);
    }
    protected void initDefault(){
        if(adapter==null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                BluetoothManager bm=(BluetoothManager) ApiServer.defaultAndroidContext.getSystemService(Context.BLUETOOTH_SERVICE);
                adapter=bm==null?null:bm.getAdapter();
                IntentFilter if2 = new IntentFilter();

                if2.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                if2.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                if2.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                if2.addAction(BluetoothDevice.ACTION_FOUND);
                if2.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
                if2.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

                //Prefer the long-lived service context; fall back to the default context.
                Context ctx=PxprpcService.current!=null?PxprpcService.current:ApiServer.defaultAndroidContext;
                try{
                    registerReceiverCompat(ctx,this,if2);
                    registeredContext=ctx;
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }else{
                throw new UnsupportedOperationException("Not support yet");
            }
        }
    }
    public void close(){
        try{
            if(i==this)i=null;
            if(registeredContext!=null){
                registeredContext.unregisterReceiver(this);
                registeredContext=null;
            }
            super.close();
        }catch(Exception e){}
    }

    public ByteBuffer describeBluetoothAdapterConstant() throws IllegalAccessException {
        return new BuiltInFuncList().listStaticConstField(BluetoothAdapter.class);
    }
    public ByteBuffer describeBluetoothDeviceConstant() throws IllegalAccessException {
        return new BuiltInFuncList().listStaticConstField(BluetoothDevice.class);
    }
    public ByteBuffer describeAdapterState(){
        TableSerializer ser = new TableSerializer().setColumnsInfo("ssic",new String[]{"address","name","state","enabled"});
        ser.addRow(new Object[]{this.adapter.getAddress(),this.adapter.getName(),this.adapter.getState(),this.adapter.isEnabled()});
        return ser.build();
    }
    public void setName(String name){
        this.adapter.setName(name);
    }
    public Bluetooth2 self(){
        return this;
    }
    public BluetoothAdapter.LeScanCallback asLeScanListener(){
        return this;
    }
    public BluetoothAdapter bluetoothAdapter(){
        return adapter;
    }

    public void requestBluetoothDicoverable(int durationSec){
        requireIntentModule().requestBluetoothDicoverable(durationSec);
    }

    public void requestEnableBluetooth(){
        requireIntentModule().requestEnableBluetooth();
    }
    protected Intent2 requireIntentModule(){
        //Previously looked up a non-existent namespace ("AndroidHelper-Intent").
        if(Intent2.i==null){
            throw new IllegalStateException("AndroidHelper.Intent module is not initialized");
        }
        return Intent2.i;
    }

    public boolean createBond(String address){
        DiscoveryResult r=discovered.get(address);
        if(r!=null && r.device!=null){
            return r.device.createBond();
        }else{
            return false;
        }
    }

    public boolean removeBond(String address) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        DiscoveryResult r=discovered.get(address);
        if(r!=null && r.device!=null){
            BluetoothDevice dev = r.device;
            Method removeBondMethod = BluetoothDevice.class.getMethod("removeBond");
            return (Boolean) removeBondMethod.invoke(dev);

        }else{
            return false;
        }
    }

    public boolean allowNoConfirmPairing=false;
    public void setAllowNoConfirmPairing(boolean b){
        allowNoConfirmPairing=b;
    }

    @SuppressLint("MissingPermission")
    public boolean setPairPin(String address, ByteBuffer pin){
        DiscoveryResult r=discovered.get(address);
        if(r==null || r.device==null){
            return false;
        }
        return r.device.setPin(Utils.toBytes(pin));
    }


    public static class DiscoveryResult{
        public BluetoothDevice device;
        public int rssi;
        public boolean pairingRequest;
        public byte[] scanRecord;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras=intent.getExtras();
        if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){
            DiscoveryResult t = null;
            BluetoothDevice dev = extras==null?null:(BluetoothDevice) extras.get(BluetoothDevice.EXTRA_DEVICE);
            String addr="";
            if(dev!=null){
                addr = dev.getAddress();
            }
            t=discovered.get(addr);
            if(t==null){
                t=new DiscoveryResult();
                discovered.put(addr,t);
            }
            t.device= dev;
            t.rssi= extras==null?0:extras.getShort(BluetoothDevice.EXTRA_RSSI,(short)0);
        }else if(BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())){
            BluetoothDevice dev = extras==null?null:(BluetoothDevice) extras.get(BluetoothDevice.EXTRA_DEVICE);
            String addr = "";
            if(dev!=null){
                addr=dev.getAddress();
            }
            DiscoveryResult t=discovered.get(addr);
            if(t==null){
                t=new DiscoveryResult();
                discovered.put(addr,t);
            }
            t.device= dev;
            t.pairingRequest=true;
            if(allowNoConfirmPairing && dev!=null){
                try{
                    dev.setPairingConfirmation(true);
                }catch(Exception ex){}
            }
        }
        //Still deliver the raw action/extra events to registered dispatchers.
        super.onReceive(context,intent);
    }

    //Written from the receiver/handler threads and read from RPC threads.
    public Map<String,DiscoveryResult> discovered =new ConcurrentHashMap<String,DiscoveryResult>();
    @SuppressLint("MissingPermission")
    public void startDiscovery(){
        bluetoothAdapter().startDiscovery();
    }
    @SuppressLint("MissingPermission")
    public void cancelDiscovery(){
        bluetoothAdapter().cancelDiscovery();
    }

    public void cleanDiscoveryResults(){
        discovered.clear();
    }

    @SuppressLint("MissingPermission")
    public ByteBuffer describeDiscoveredDevices(){
        TableSerializer ser = new TableSerializer().setColumnsInfo(null, new String[]{
                "address", "class", "name", "rssi", "type", "bondState","scanRecord"});
        for(Map.Entry<String,DiscoveryResult> e:this.discovered.entrySet()){
            DiscoveryResult v = e.getValue();
            BluetoothDevice dev=v.device;
            if(dev==null){
                continue;
            }
            ser.addRow(new Object[]{
                    e.getKey(),dev.getBluetoothClass()==null?0:dev.getBluetoothClass().getDeviceClass(),dev.getName(),
                    v.rssi,dev.getType(),dev.getBondState(),v.scanRecord!=null?ByteBuffer.wrap(v.scanRecord):ByteBuffer.allocate(0)});
        }
        return ser.build();
    }
    @SuppressLint("MissingPermission")
    public ByteBuffer describeDiscoveredDevice(String address){
        TableSerializer ser = new TableSerializer().setColumnsInfo("sisiii", new String[]{"address", "class", "name", "rssi", "type", "bondState"});
        DiscoveryResult dr = this.discovered.get(address);
        if(dr!=null && dr.device!=null){
            ser.addRow(new Object[]{
                    address,dr.device.getBluetoothClass()==null?0:dr.device.getBluetoothClass().getDeviceClass(),dr.device.getName(),
                    dr.rssi,dr.device.getType(),dr.device.getBondState()});
        }
        return ser.build();
    }
    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        if(bluetoothDevice==null){
            return;
        }
        String addr=bluetoothDevice.getAddress();
        DiscoveryResult t = discovered.get(addr);
        if(t==null){
            t=new DiscoveryResult();
            discovered.put(addr,t);
        }
        t.device = bluetoothDevice;
        t.rssi=rssi;
        t.scanRecord=scanRecord;
    }

    public BluetoothServerSocket listenRfcomm(String name, String uuid) throws IOException {
        return adapter.listenUsingInsecureRfcommWithServiceRecord(name, UUID.fromString(uuid));
    }
    public BluetoothServerSocket listenRfcommSecure(String name, String uuid) throws IOException {
        return adapter.listenUsingRfcommWithServiceRecord(name, UUID.fromString(uuid));
    }
    public BluetoothSocket connectRfcomm(String address,String uuid) throws IOException {
        DiscoveryResult r=this.discovered.get(address);
        if(r==null || r.device==null){
            throw new IOException("Unknown device: "+address+", run a discovery first.");
        }
        BluetoothSocket s = r.device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid));
        s.connect();
        return s;
    }
    public BluetoothSocket connectRfcommSecure(String address,String uuid) throws IOException {
        DiscoveryResult r=this.discovered.get(address);
        if(r==null || r.device==null){
            throw new IOException("Unknown device: "+address+", run a discovery first.");
        }
        BluetoothSocket s = r.device.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
        s.connect();
        return s;
    }
    public ByteBuffer querySupportUuids(String address){
        DiscoveryResult r=this.discovered.get(address);
        ParcelUuid[] uuids = r==null || r.device==null?null:r.device.getUuids();
        Serializer2 ser=new Serializer2().prepareSerializing(32);
        ser.putVarint(uuids==null?0:uuids.length);
        if(uuids!=null){
            for(ParcelUuid e:uuids){
                ser.putString(e.getUuid().toString());
            }
        }
        return ser.build();
    }
    @TargetApi(Build.VERSION_CODES.Q)
    public BluetoothServerSocket listenL2cap() throws IOException {
        ensureL2capSupported();
        return adapter.listenUsingInsecureL2capChannel();
    }
    @TargetApi(Build.VERSION_CODES.Q)
    public BluetoothServerSocket listenL2capSecure() throws IOException {
        ensureL2capSupported();
        return adapter.listenUsingL2capChannel();
    }
    protected void ensureL2capSupported(){
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
            throw new UnsupportedOperationException("L2CAP CoC requires Android 10 (API 29)");
        }
    }
    public BluetoothSocket socketAccept(AsyncReturn<BluetoothSocket> ret,BluetoothServerSocket s, int timeout) {
        PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ret.resolve(s.accept(timeout));
                } catch (Exception e) {
                    ret.reject(e);
                }
            }
        });
        return null;
    }
    public ByteBuffer socketRead(BluetoothSocket s) throws IOException {
        byte[] b = new byte[4096];
        int read=s.getInputStream().read(b);
        if(read<0){
            //EOF: an empty buffer lets the caller detect the closed stream.
            return ByteBuffer.allocate(0);
        }
        return ByteBuffer.wrap(b,0,read);
    }
    public void socketWrite(BluetoothSocket s,ByteBuffer b) throws IOException {
        s.getOutputStream().write(Utils.toBytes(b));
    }
    public void socketClose(Closeable s) throws IOException {
        s.close();
    }
}
