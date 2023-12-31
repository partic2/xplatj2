package project.xplat.launcher.pxprpcapi.androidhelper;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import project.xplat.launcher.pxprpcapi.ApiServer;

import java.io.Closeable;
import java.util.UUID;

public class SysBase {

	public static final String PxprpcNamespace="AndroidHelper-Sysbase";

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

	public void close(Closeable c) {
		ApiServer.closeQuietly(c);
	}
}
