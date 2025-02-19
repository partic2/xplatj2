package pxprpcapi.androidhelper;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import project.xplat.launcher.ApiServer;
import project.xplat.launcher.AssetsCopy;
import pxprpc.base.Utils;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;
import pxprpc.extend.TypeDeclParser;
import xplatj.gdxconfig.core.PlatCoreConfig;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class SysBase implements Closeable{

	public static final String PxprpcNamespace="AndroidHelper.Sysbase";
	public static SysBase i;

	public ActivityManager am;
	public PackageManager pm;
	public SysBase(){
		i=this;
		am=(ActivityManager)ApiServer.defaultAndroidContext.getSystemService(Context.ACTIVITY_SERVICE);
		pm=ApiServer.defaultAndroidContext.getPackageManager();
	}

	public BroadcastReceiver newBroadcastReceiver() {
		return new PxprpcBroadcastReceiverAdapter();
	}

	public Context getDefaultContext() {
		return ApiServer.defaultAndroidContext;
	}

	public void registerBroadcastReceiver(BroadcastReceiver receiver, String filter) {
		getDefaultContext().registerReceiver(receiver, new IntentFilter(filter));
	}

	public void unregisterBroadcastReceiver(BroadcastReceiver receiver) {
		getDefaultContext().unregisterReceiver(receiver);
	}


	public Object getService(String name) {
		return getDefaultContext().getSystemService(name);
	}

	public UUID newUUID(long mostSigBits, long leastSigBits) {
		return new UUID(mostSigBits, leastSigBits);
	}
	public void requestExit(){
		if(ApiServer.defaultAndroidContext instanceof Service){
			((Service) ApiServer.defaultAndroidContext).stopSelf();
		}else if(ApiServer.defaultAndroidContext instanceof Activity){
			((Activity) ApiServer.defaultAndroidContext).finish();
		}
	}
	public ByteBuffer deviceInfo(){
		return new TableSerializer().setColumnInfo(null,new String[]{
				"version","hardware","abi","product","device","board","manufacturer","brand"}).addRow(new Object[]{
				Build.VERSION.SDK_INT,Build.HARDWARE, Utils.stringJoin(",", Arrays.asList(Build.SUPPORTED_ABIS)),
				Build.PRODUCT,Build.DEVICE,Build.BOARD,Build.MANUFACTURER,Build.BRAND}).build();
	}

	@MethodTypeDecl("->lllc")
	public Object[] getMemoryInfo(){
		ActivityManager.MemoryInfo meminfo=new ActivityManager.MemoryInfo();
		am.getMemoryInfo(meminfo);
		return new Object[]{meminfo.availMem,meminfo.totalMem,meminfo.threshold,meminfo.lowMemory};
	}
	public String getDataDir(){
		return AssetsCopy.assetsDir;
	}
	public String getHostPackageName(){
		return ApiServer.defaultAndroidContext.getPackageName();
	}

	public ByteBuffer dumpBundle(Bundle b){
		HashMap<String,Object> bundleData=new HashMap<String,Object>();
		for(String key:b.keySet()){
			Object val=b.get(key);
			if(TypeDeclParser.jtypeToValueInfo(val.getClass())!='o'){
				bundleData.put(key,val);
			}
		}
		ArrayList<Map<String, Object>> t1 = new ArrayList<Map<String, Object>>();
		t1.add(bundleData);
		return new TableSerializer().fromMapArray(t1).build();
	}

	@Override
	public void close() throws IOException {
		if(i==this)i=null;
	}
}
