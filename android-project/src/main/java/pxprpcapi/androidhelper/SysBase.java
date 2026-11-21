package pxprpcapi.androidhelper;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import partic2.pxseedloader.android.launcher.ApiServer;
import partic2.pxseedloader.android.launcher.AssetsCopy;
import partic2.pxseedloader.android.launcher.MainActivity;
import partic2.pxseedloader.android.launcher.PxprpcService;
import pxprpc.base.Utils;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.EventDispatcher;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;
import pxprpc.extend.TypeDeclParser;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

	public PxprpcBroadcastReceiverAdapter newBroadcastReceiver() {
		return new PxprpcBroadcastReceiverAdapter();
	}

	public Context getDefaultContext() {
		return ApiServer.defaultAndroidContext;
	}
	public Context getServiceContext(){return PxprpcService.current;}

	public void registerBroadcastReceiver(BroadcastReceiver receiver, String filter) {
		PxprpcBroadcastReceiverAdapter.registerReceiverCompat(getDefaultContext(), receiver, new IntentFilter(filter));
	}

	public void unregisterBroadcastReceiver(BroadcastReceiver receiver) {
		getDefaultContext().unregisterReceiver(receiver);
	}

	public EventDispatcher registerPxprpcBroadcastReceiver(String filter){
		PxprpcBroadcastReceiverAdapter receiver = newBroadcastReceiver();
		return receiver.autoUnregisterEventDispatcher(getDefaultContext(),filter);
	}

	public EventDispatcher registerServicePxprpcBroadcastReceiver(String filter){
		PxprpcBroadcastReceiverAdapter receiver = newBroadcastReceiver();
		return receiver.autoUnregisterEventDispatcher(PxprpcService.current,filter);
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
		return new TableSerializer().setColumnsInfo(null,new String[]{
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

	public static ByteBuffer dumpBundle(Bundle b){
		HashMap<String,Object> bundleData=new HashMap<String,Object>();
		for(String key:b.keySet()){
			Object val=b.get(key);
			//Bundle values can legitimately be null; keep a placeholder so that
			//TableSerializer's type guessing (which reads getClass()) never NPEs.
			if(val==null){
				bundleData.put(key,"null");
				continue;
			}
			if(TypeDeclParser.jtypeToValueInfo(val.getClass())!='o'){
				bundleData.put(key,val);
			}else{
				bundleData.put(key,val.toString());
			}
		}
		ArrayList<Map<String, Object>> t1 = new ArrayList<Map<String, Object>>();
		t1.add(bundleData);
		return new TableSerializer().fromMapArray(t1).build();
	}

	public String copyAssets(final AsyncReturn<String> ret,final String sourceDir,final String destinationDir){
		new Thread(new Runnable() {
			@Override
			public void run() {
                try {
                    AssetsCopy.CopyFiles(sourceDir,destinationDir);
					ret.resolve("");
                } catch (Exception e) {
                    ret.reject(e);
                }

            }
		}).start();
		return "";
	}

	public static String readApkAssetAsText(String fileName) throws IOException {
		Context context = ApiServer.defaultAndroidContext;
		InputStream is=null;
		try {
			is = context.getAssets().open(fileName);
			ByteArrayOutputStream baos = new ByteArrayOutputStream() ;
			byte[] buffer = new byte[8192];
			int len;
			while ((len = is.read(buffer)) != -1) {
				baos.write(buffer, 0, len);
			}
			return baos.toString("utf-8");
		}finally{
			if(is!=null){
				is.close();
			}
		}
	}


	@Override
	public void close() throws IOException {
		if(i==this)i=null;
	}
}
