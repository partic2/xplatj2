package project.xplat.launcher;


import android.content.Context;
import android.content.res.AssetManager;

import java.io.*;

public class AssetsCopy {
	private Context mContext;
	public static AssetsCopy ac;
	public static String assetsDir="/sdcard/xplat";
	public static String packageName="project.xplat";
	public static boolean loadAssets(Context ctx) throws IOException {
		File f=new File(assetsDir+"/flat");
		if(f.exists()){
			return false;
		}else{
			ac=new AssetsCopy(ctx);
			ac.CopyFiles("res",assetsDir);
			return true;
		}
	}
	public AssetsCopy(Context context) {
		mContext = context;
	}

	public boolean CopyFiles(String oldPath, String newPath) throws IOException {
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
