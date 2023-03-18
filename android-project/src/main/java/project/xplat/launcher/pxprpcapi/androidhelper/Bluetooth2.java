package project.xplat.launcher.pxprpcapi.androidhelper;

import android.annotation.TargetApi;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.ParcelUuid;
import project.xplat.launcher.pxprpcapi.ApiServer;
import project.xplat.launcher.pxprpcapi.Utils;
import pursuer.patchedmsgpack.tools.ArrayBuilder2;
import pursuer.patchedmsgpack.tools.MPValueTable;
import pursuer.patchedmsgpack.tools.MapBuilder2;
import pursuer.pxprpc.EventDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Bluetooth2 extends PxprpcBroadcastReceiverAdapter implements BluetoothAdapter.LeScanCallback,Closeable {
    public static final String PxprpcNamespace="AndroidHelper-Bluetooth";
    public BluetoothAdapter adapter;
    public Bluetooth2(){
        init();
    }
    public void init(){
        if(adapter==null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                adapter=((BluetoothManager) ApiServer.defaultAndroidContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
                IntentFilter if2 = new IntentFilter();

                if2.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                if2.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                if2.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                if2.addAction(BluetoothDevice.ACTION_FOUND);
                if2.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
                if2.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

                ApiServer.defaultAndroidContext.registerReceiver(this, if2);
            }else{
                throw new UnsupportedOperationException("Not support yet");
            }
        }
    }
    public void close(){

        try{
            ApiServer.defaultAndroidContext.unregisterReceiver(this);
        }catch(Exception e){}
    }

    public byte[] describeBluetoothAdapterConstant(){
        return ApiServer.sysbase.describeStaticFields(BluetoothAdapter.class);
    }
    public byte[] describeBluetoothDeviceConstant(){
        return ApiServer.sysbase.describeStaticFields(BluetoothDevice.class);
    }
    public byte[] describeAdapterState(){
        MapBuilder2 mb=new MapBuilder2()
                .put("address",this.adapter.getAddress())
                .put("name",this.adapter.getName())
                .put("state",this.adapter.getState())
                .put("enabled",this.adapter.isEnabled());
        return Utils.packFrom(mb.build());
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
        ((Intent2)ApiServer.getModule("AndroidHelper-Intent")).requestBluetoothDicoverable(durationSec);
    }

    public void requestEnableBluetooth(){
        ((Intent2)ApiServer.getModule("AndroidHelper-Intent")).requestEnableBluetooth();
    }

    public boolean createBond(String address){
        if(discovered.containsKey(address)){
            return discovered.get(address).device.createBond();
        }else{
            return false;
        }
    }

    public boolean removeBond(String address) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if(discovered.containsKey(address)){
            BluetoothDevice dev = this.discovered.get(address).device;
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

    public boolean setPairPin(String address,byte[] pin){
        return discovered.get(address).device.setPin(pin);
    }


    public static class DiscoveryResult{
        public BluetoothDevice device;
        public int rssi;
        public boolean pairingRequest;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){
            DiscoveryResult t = new DiscoveryResult();
            BluetoothDevice dev = (BluetoothDevice) intent.getExtras().get(BluetoothDevice.EXTRA_DEVICE);
            String addr = t.device.getAddress();
            if(discovered.containsKey(addr)){
                t=discovered.get(addr);
            }else{
                discovered.put(addr,t);
            }
            t.device= dev;
            t.rssi= intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,(short)0);
        }else if(BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())){
            DiscoveryResult t = new DiscoveryResult();
            BluetoothDevice dev = (BluetoothDevice) intent.getExtras().get(BluetoothDevice.EXTRA_DEVICE);
            String addr = t.device.getAddress();
            if(discovered.containsKey(addr)){
                t=discovered.get(addr);
            }else{
                discovered.put(addr,t);
            }
            t.device= dev;
            t.pairingRequest=true;
            if(allowNoConfirmPairing){
                t.device.setPairingConfirmation(true);
            }
        }
        this.dispatcher.fireEvent(intent.getAction());
    }

    public HashMap<String,DiscoveryResult> discovered =new HashMap<String,DiscoveryResult>();
    public void startDiscovery(){
        bluetoothAdapter().startDiscovery();
    }
    public void cancelDiscovery(){
        bluetoothAdapter().cancelDiscovery();
    }

    public void cleanDiscoveryResults(){
        discovered.clear();
    }

    public byte[] describeDiscoveredDevices(){
        MPValueTable vt = new MPValueTable().header(new String[]{"address","class","name","rssi","type","bondState"});
        for(Map.Entry<String,DiscoveryResult> e:this.discovered.entrySet()){
            DiscoveryResult v = e.getValue();
            vt.addRow(new ArrayBuilder2()
                    .add(e.getKey()).add(v.device.getBluetoothClass().getDeviceClass()).add(v.device.getName()).add(v.rssi).add(v.device.getType())
                            .add(v.device.getBondState())
                    .build());
        }
        return Utils.packFrom(vt.toValue());
    }
    public byte[] describeDiscoveredDevice(String address){
        DiscoveryResult v = this.discovered.get(address);
        return Utils.packFrom(new ArrayBuilder2()
                .add(address).add(v.device.getBluetoothClass().getDeviceClass()).add(v.device.getName()).add(v.rssi).add(v.device.getType())
                .add(v.device.getBondState())
                .build());
    }
    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        DiscoveryResult t = new DiscoveryResult();
        t.device = bluetoothDevice;
        t.pairingRequest=false;
        t.rssi=rssi;
        discovered.put(bluetoothDevice.getAddress(),t);
    }

    public BluetoothServerSocket listenRfcomm(String name, String uuid) throws IOException {
        return adapter.listenUsingInsecureRfcommWithServiceRecord(name, UUID.fromString(uuid));
    }
    public BluetoothServerSocket listenRfcommSecure(String name, String uuid) throws IOException {
        return adapter.listenUsingRfcommWithServiceRecord(name, UUID.fromString(uuid));
    }
    public BluetoothSocket connectRfcomm(String address,String uuid) throws IOException {
        BluetoothSocket s = this.discovered.get(address).device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid));
        s.connect();
        return s;
    }
    public BluetoothSocket connectRfcommSecure(String address,String uuid) throws IOException {
        BluetoothSocket s = this.discovered.get(address).device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid));
        s.connect();
        return s;
    }
    public byte[] querySupportUuids(String address){
        BluetoothDevice dev = discovered.get(address).device;
        ParcelUuid[] uuids = dev.getUuids();
        ArrayBuilder2 v = new ArrayBuilder2();
        for(ParcelUuid e:uuids){
            v.add(e.getUuid().toString());
        }
        return Utils.packFrom(v.build());
    }
    @TargetApi(Build.VERSION_CODES.Q)
    public BluetoothServerSocket listenL2cap() throws IOException {
        return adapter.listenUsingInsecureL2capChannel();
    }
    @TargetApi(Build.VERSION_CODES.Q)
    public BluetoothServerSocket listenL2capSecure() throws IOException {
        return adapter.listenUsingL2capChannel();
    }
    public BluetoothSocket socketAccept(BluetoothServerSocket s,int timeout) throws IOException {
        return s.accept(timeout);
    }
    public byte[] socketRead(BluetoothSocket s) throws IOException {
        byte[] b = new byte[4096];
        int read=s.getInputStream().read(b);
        return Arrays.copyOf(b,read);
    }
    public void socketWrite(BluetoothSocket s,byte[] b) throws IOException {
        s.getOutputStream().write(b);
    }
    public void socketClose(Closeable s) throws IOException {
        s.close();
    }
}
