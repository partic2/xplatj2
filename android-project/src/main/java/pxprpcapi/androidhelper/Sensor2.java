package pxprpcapi.androidhelper;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.*;
import android.os.Build;
import project.xplat.launcher.ApiServer;
import pxprpc.base.Serializer2;
import pxprpc.extend.EventDispatcher;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class Sensor2 extends EventDispatcher implements SensorEventListener, Closeable {

    public static final String PxprpcNamespace="AndroidHelper.Sensor";
    public static Sensor2 i;

    private final SensorManager smgr;
    public Sensor2(){
        this.smgr=(SensorManager) ApiServer.defaultAndroidContext.getSystemService(Context.SENSOR_SERVICE);
        i=this;
    }
    public List<Sensor> getSensorList(int filter){
        List<Sensor> sl = this.smgr.getSensorList(filter);
        return sl;
    }
    public String listSensorFilter(){
        StringBuilder sb = new StringBuilder();
        Class<Sensor> cls = Sensor.class;
        for(Field f:cls.getDeclaredFields()){
            if(f.getName().startsWith("TYPE_")){
                try {
                    sb.append(f.getName()).append(":").append(f.getInt(null)).append("\n");
                } catch (IllegalAccessException e) {
                }
            }
        }
        return sb.toString();
    }
    public String uid="";
    public String getUid(){
        return uid;
    }
    public void setUid(String uid){
        this.uid=uid;
    }
    public Sensor2 self(){
        return this;
    }
    public Map<Sensor,Integer> runningSensor=new HashMap<Sensor,Integer>();
    protected int lastRunningSensorId=0;
    public int sensorStart(Sensor sensor,int samplePeriod) {
        //int SENSOR_DELAY_FASTEST = 0;
        //int SENSOR_DELAY_GAME = 1;
        //int SENSOR_DELAY_UI = 2;
        //int SENSOR_DELAY_NORMAL = 3;
        smgr.registerListener(this,sensor,samplePeriod);
        lastRunningSensorId++;
        runningSensor.put(sensor,lastRunningSensorId);
        return lastRunningSensorId;
    }
    public void sensorStop(Sensor sensor){
        smgr.unregisterListener(this,sensor);
        runningSensor.remove(sensor);
    }
    public void sensorStopAll(){
        for(Sensor s:runningSensor.keySet()){
            smgr.unregisterListener(this,s);
        }
        runningSensor.clear();
        lastRunningSensorId=0;
    }
    public Map<Sensor,Integer> getRunningSensor(){
        return this.runningSensor;
    }
    public String sendorListNames(List<Sensor> sensorList) {
        StringBuilder sb=new StringBuilder();
        for(Sensor s:sensorList){
            sb.append(s.getName()+"\n");
        }
        return sb.toString();
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        this.fireEvent(event);
    }

    public ByteBuffer packSensorEvent(SensorEvent event){
        Serializer2 ser = new Serializer2().prepareSerializing(64);
        ser.putInt(runningSensor.get(event.sensor));
        ser.putInt(event.values.length);
        for(int i=0;i<event.values.length;i++){
            ser.putFloat(event.values[i]);
        }
        return ser.build();
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
        this.sensorStopAll();
    }
}
