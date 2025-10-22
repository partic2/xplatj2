package project.xplat.launcher;


import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import pxprpc.extend.RpcExtendClientCallable;
import pxprpc.extend.TableSerializer;
import pxprpc.runtimebridge.NativeHelper;
import pxprpc.runtimebridge.PipeServer;
import pxprpc.runtimebridge.RuntimeBridgeUtils;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.partic2.filesystem.impl.PrefixFS;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class AssetsCopy {
	private static Context mContext;
	public static AssetsCopy ac;
	//Will be override to app internal files.
	public static String assetsDir="/sdcard/Download/xplat";
	public static boolean loadAssets(Context ctx) throws IOException {
		File f=new File(assetsDir+"/xplat-flag.txt");
		if(f.exists()){
			return false;
		}else{
			CopyFiles("res",assetsDir);
			return true;
		}
	}
	public static PipeServer rtbPipeServ;
	public static void init(Context context){
		try {
			ByteBuffer errorMessage= ByteBuffer.allocateDirect(255);
			NativeHelper.loadNativeLibrary();
			NativeHelper.ensureRtbInited(errorMessage);
			if(errorMessage.duplicate().get()!=0){
				Log.e("pxprpc_rtbridge","pxprpc_rtbridge init failed.");
			}else{
				RuntimeBridgeUtils.ensureInit();
				RuntimeBridgeUtils.registerJavaPipeServer();
			}
			mContext=context;
			AssetsCopy.assetsDir=mContext.getFilesDir().getCanonicalPath();
			AssetsCopy.loadAssets(context);
			PrefixFS.defaultPrefix=AssetsCopy.assetsDir+"/";
			PlatCoreConfig.platApi=new PlatApiImpl(context);
			if(PlatCoreConfig.get()==null){
				PlatCoreConfig.singleton.set(new PlatCoreConfig());
			}
			RpcExtendClientCallable setAndroidInitInfo = RuntimeBridgeUtils.client.getFunc("pxprpc_PxseedLoader.setAndroidInitInfo");
			String processTag="";
			if(context instanceof PxprpcService){
				processTag="pxprpcsrv";
			}else if(context instanceof Activity){
				processTag="activity";
			}
			setAndroidInitInfo.typedecl("sis->").callBlock(AssetsCopy.assetsDir, Build.VERSION.SDK_INT,processTag);
			setAndroidInitInfo.free();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean CopyFiles(String oldPath, String newPath) throws IOException {
		boolean isCopy = true;
		AssetManager mAssetManger = mContext.getAssets();
		String[] fileNames=mAssetManger.list(oldPath);

		if (fileNames.length > 0) {
			File file = new File(newPath);
			file.mkdirs();
			for (String fileName : fileNames) {
				if(oldPath=="")
					CopyFiles(fileName,newPath+"/"+fileName);
				else
					CopyFiles(oldPath+"/"+fileName,newPath+"/"+fileName);
			}
		}else {
			InputStream is = mAssetManger.open(oldPath);
			FileOutputStream fos = new FileOutputStream(new File(newPath));
			new File(newPath).setReadable(true,false);
			new File(newPath).setWritable(true,false);
			byte[] buffer = new byte[1024];
			int byteCount=0;
			while((byteCount=is.read(buffer))!=-1) {
				fos.write(buffer, 0, byteCount);
			}
			fos.flush();
			is.close();
			fos.close();
		}
		return isCopy;
	}

}
