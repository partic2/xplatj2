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
import xplatj.platform.PlatApi;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;



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
	public static Boolean startOptsParsed=false;
	public static void ensureStartOpts(){
		
		synchronized (startOptsParsed){
			if(startOptsParsed)return;
			startOptsParsed=true;
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
			AssetsCopy.loadAssets(this);
		} catch (IOException e) {
			e.printStackTrace();
			finish();
		}
		launch();
	}
	String[] dangerousPerm=new String[]{"android.permission.READ_CALENDAR","android.permission.WRITE_CALENDAR",
			"android.permission.CAMERA","android.permission.READ_CONTACTS","android.permission.WRITE_CONTACTS",
			"android.permission.GET_ACCOUNTS","android.permission.ACCESS_FINE_LOCATION","android.permission.RECORD_AUDIO",
			"android.permission.READ_PHONE_STATE","android.permission.CALL_PHONE","android.permission.READ_CALL_LOG",
			"android.permission.WRITE_CALL_LOG","android.permission.ADD_VOICEMAIL","android.permission.USE_SIP",
			"android.permission.BODY_SENSORS","android.permission.SEND_SMS","android.permission.RECEIVE_SMS",
			"android.permission.READ_SMS","android.permission.RECEIVE_WAP_PUSH","android.permission.RECEIVE_MMS",
			"android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE"};



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
		//ensure this is called when every activity created
		AssetsCopy.init(this);
		startOptsParsed=false;
		MainActivity.context = this.getApplicationContext();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			String[] reqPerms=getPermissionNotGranted();
			if(reqPerms.length>0) this.requestPermissions(reqPerms,1);
		}else {
			initEnviron();
		}
	}
	public void launch(){
		try {
			ensureStartOpts();

			if(sdlFlag.equals(selectedBackend)){
				intent=new Intent();
				intent.setClass(this,Class.forName("project.sdl.MainActivity"));
				this.startActivityForResult(intent,1);
			}else if(webappFlag.equals(selectedBackend)){
				intent=new Intent();
				intent.setClass(this,Class.forName("project.webapp.MainActivity"));
				this.startActivityForResult(intent,1);
			}
		} catch (ClassNotFoundException e) {
			finish();
		}
		
	}
	@Override
	protected void onDestroy() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
			multicastLock.release();
		}
		super.onDestroy();
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		boolean shutdown=true;
		try{
			startOptsParsed=false;
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
