package project.xplat.launcher;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Build;
import android.os.Bundle;
import pxprpcapi.androidhelper.AndroidUIBase;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.partic2.util.AsyncFuncChain;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


public class MainActivity extends Activity {
	/** Called when the activity is first created. */
	public static Context context;
	private Intent intent;
	public static String gdxFlag="gdx";
	public static String sdlFlag="sdl";
	public static String webappFlag="webapp";
	public static String shutdownFlag="shutdown";
	public static String rebootFlag="reboot";

	public static String selectedBackend="";
	public static String selectedResultAction="shutdown";
	public static boolean debugMode=false;
	public static boolean[] startOptsParsed=new boolean[]{false};
	public static Integer currentTaskId=null;

	public static void ensureStartOpts(){
		
		synchronized (startOptsParsed){
			if(startOptsParsed[0])return;
			startOptsParsed[0]=true;
			FileInputStream in1 = null;
			try {
				in1 = new FileInputStream(AssetsCopy.assetsDir + "/xplat-flag.txt");
				byte[] content=new byte[1024];
				int len=in1.read(content);
				String[] opts=new String(content,0,len, StandardCharsets.UTF_8).split("\\s+");
				for(String opt:opts){
					if("debug".equals(opt)){
						debugMode=true;
					}else if("launcher_end".equals(opt)) {
						break;
					}
				}
				selectedBackend=opts[0];
				selectedResultAction=opts[1];
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}finally{
				if(in1!=null){
					try {
						in1.close();
					} catch (IOException ex) {
					}
				}
			}
		}
	}
	
	MulticastLock multicastLock;

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		initEnviron();
	}

	public void initEnviron(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				try{
					Runtime.getRuntime().exec("chmod 0777 " + context.getFilesDir().getAbsolutePath());
				}
				catch (IOException e) {}

				if(Build.VERSION.SDK_INT>=19){
					context.getExternalFilesDirs(null);
				}
				WifiManager wifiMgr = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
					multicastLock = wifiMgr.createMulticastLock("xplat");
					multicastLock.setReferenceCounted(false);
					multicastLock.acquire();
				}
				try {
					AssetsCopy.loadAssets(MainActivity.this);
				} catch (IOException e) {
					e.printStackTrace();
					finish();
				}
				MainActivity.this.startService(new Intent(MainActivity.this,PxprpcService.class));
				launch();
			}
		}).start();
	}
	String[] dangerousPerm=new String[]{"android.permission.ACCESS_LOCATION_EXTRA_COMMANDS","android.permission.ACCESS_NETWORK_STATE",
			"android.permission.ACCESS_NOTIFICATION_POLICY","android.permission.ACCESS_WIFI_STATE","android.permission.BLUETOOTH",
			"android.permission.BLUETOOTH_ADMIN","android.permission.BLUETOOTH_ADVERTISE","android.permission.BLUETOOTH_CONNECT",
			"android.permission.BROADCAST_STICKY","android.permission.CHANGE_NETWORK_STATE","android.permission.CHANGE_WIFI_MULTICAST_STATE",
			"android.permission.CHANGE_WIFI_STATE","android.permission.DISABLE_KEYGUARD","android.permission.EXPAND_STATUS_BAR",
			"android.permission.GET_PACKAGE_SIZE","com.android.launcher.permission.INSTALL_SHORTCUT","android.permission.INTERNET",
			"android.permission.KILL_BACKGROUND_PROCESSES","android.permission.MODIFY_AUDIO_SETTINGS","android.permission.NFC",
			"android.permission.READ_SYNC_SETTINGS","android.permission.READ_SYNC_STATS","android.permission.RECEIVE_BOOT_COMPLETED",
			"android.permission.REORDER_TASKS","android.permission.REQUEST_INSTALL_PACKAGES","com.android.alarm.permission.SET_ALARM",
			"android.permission.SET_WALLPAPER","android.permission.SET_WALLPAPER_HINTS","android.permission.TRANSMIT_IR",
			"com.android.launcher.permission.UNINSTALL_SHORTCUT","android.permission.USE_BIOMETRIC","android.permission.VIBRATE",
			"android.permission.WAKE_LOCK","android.permission.WRITE_SYNC_SETTINGS","android.permission.READ_CALENDAR",
			"android.permission.WRITE_CALENDAR","android.permission.CAMERA","android.permission.FLASHLIGHT","android.permission.READ_CONTACTS",
			"android.permission.WRITE_CONTACTS","android.permission.GET_ACCOUNTS","android.permission.ACCESS_FINE_LOCATION",
			"android.permission.RECORD_AUDIO","android.permission.READ_PHONE_STATE","android.permission.CALL_PHONE",
			"android.permission.READ_CALL_LOG","android.permission.WRITE_CALL_LOG","com.android.voicemail.permission.ADD_VOICEMAIL",
			"android.permission.USE_SIP","android.permission.BODY_SENSORS","android.permission.SEND_SMS","android.permission.RECEIVE_SMS",
			"android.permission.READ_SMS","android.permission.RECEIVE_WAP_PUSH","android.permission.RECEIVE_MMS",
			"android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE","android.permission.MANAGE_EXTERNAL_STORAGE",
			"android.permission.SYSTEM_ALERT_WINDOW","android.permission.SYSTEM_OVERLAY_WINDOW","android.permission.ACCESS_COARSE_LOCATION",
			"android.permission.BLUETOOTH_SCAN"};



	@TargetApi(Build.VERSION_CODES.M)
	public String[] getPermissionNotGranted(){
		ArrayList<String> permNotGranted=new ArrayList<String>();
		for(String perm:dangerousPerm){
			if(!(this.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED)){
				permNotGranted.add(perm);
			}
		}
		return permNotGranted.toArray(new String[0]);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		currentTaskId=getTaskId();
		AssetsCopy.init(this);
		ApiServer.start(this);
		startOptsParsed[0]=false;
		MainActivity.context = this.getApplicationContext();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			String[] reqPerms=getPermissionNotGranted();
			if(reqPerms.length>0) this.requestPermissions(reqPerms,1);
		}else {
			initEnviron();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		ApiServer.defaultAndroidContext = this;
	}

	public void launch(){
		try {
			ensureStartOpts();

			if(sdlFlag.equals(selectedBackend)){
				intent=new Intent();
				intent.setClass(this,Class.forName("project.xplat.sdl.MainActivity"));
				this.startActivityForResult(intent,1);
			}else if(webappFlag.equals(selectedBackend)){
				intent=new Intent();
				intent.setClass(this,Class.forName("project.xplat.webapp.MainActivity"));
				this.startActivityForResult(intent,1);
			}
		} catch (ClassNotFoundException e) {
			finish();
		}
		
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		ApiServer.stop();
		currentTaskId=null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
			multicastLock.release();
		}
		super.onDestroy();
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		boolean shutdown=true;
		ApiServer.defaultAndroidContext=this;
		try{
			startOptsParsed[0]=false;
			ensureStartOpts();
			if(shutdownFlag.equals(selectedResultAction)){
				shutdown=true;
			}else if(rebootFlag.equals(selectedResultAction)){
				shutdown=false;
			}
		}catch(Exception e){
		}
		if(!shutdown){
			launch();
		}else{
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
