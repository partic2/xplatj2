package project.xplat.launcher;


import android.content.Context;
import android.content.res.AssetManager;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.pursuer.filesystem.impl.PrefixFS;

import java.io.*;

public class AssetsCopy {
	private static Context mContext;
	public static AssetsCopy ac;
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
	public static void init(Context context){
		try {
			mContext=context;
			AssetsCopy.assetsDir=mContext.getFilesDir().getCanonicalPath();
			PrefixFS.defaultPrefix=AssetsCopy.assetsDir+"/";
			PlatCoreConfig.platApi=new PlatApiImpl(context);
			if(PlatCoreConfig.get()==null){
				PlatCoreConfig.singleton.set(new PlatCoreConfig());
			}
		} catch (IOException e) {
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
