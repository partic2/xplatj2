package project.xplat.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;

import android.content.Context;
import android.util.Base64;
import dalvik.system.DexClassLoader;
import xplatj.javaplat.pursuer.io.stream.StreamTransmit;
import xplatj.platform.PlatApi;

public class PlatApiImpl implements PlatApi{
	public Context ctx;
	public PlatApiImpl(Context ctx){
		this.ctx=ctx;
	}
	private void copyFile(File src, File dest) throws IOException {
		FileInputStream srcStream = new FileInputStream(src);
		FileOutputStream destStream = new FileOutputStream(dest, false);
		new StreamTransmit(null, srcStream, destStream, srcStream.available(), 0x3000, null);
		srcStream.close();
		destStream.close();
	}
	@Override
	public ClassLoader load(File[] obj, ClassLoader parent) {
		DexClassLoader dex;
		String cache=cacheDir().getAbsolutePath();
		StringBuilder cp=new StringBuilder();
		HashSet<String> fileNames=new HashSet<String>();
		int cacheFileId=0;
		File newTmpDexDir=null;
		for (File f : obj) {
			String name = f.getName();
			if (fileNames.contains(name)) {
				String dotSuffix=name.substring(name.lastIndexOf("."));
				if (newTmpDexDir == null) {
					newTmpDexDir = new File(cacheDir(), "tmpDex");
					newTmpDexDir.mkdirs();
				}
				File newDexFile = new File(newTmpDexDir, f.getAbsolutePath().hashCode() + "-" + cacheFileId + dotSuffix);
				for (;newDexFile.exists();cacheFileId++) {}
				cacheFileId++;
				try {
					copyFile(f, newDexFile);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
				f = newDexFile;
			} else {
				fileNames.add(name);
			}
			cp = cp.append(f.getAbsolutePath()).append(":");
		}
		int cplen=cp.length();
		if (cplen > 0 && cp.charAt(cplen - 1) == ':') {
			cp.deleteCharAt(cplen - 1);
		}
		try {
			String dirprefix=Base64.encodeToString(MessageDigest.getInstance("MD5").digest(cp.toString().getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT).replace('/', '-');
			File odexdir=new File(cache + "/" + dirprefix);
			odexdir.mkdirs();
			dex = new DexClassLoader(cp.toString(), odexdir.getAbsolutePath() , System.getProperty("libraryPath"), parent);
			return dex;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public File cacheDir() {
		return ctx.getCacheDir();
	}

	@Override
	public String[] getStoragePathList() {
		ArrayList<String> paths=new ArrayList<String>();
		if(android.os.Build.VERSION.SDK_INT>=19){
			File[] dirs=ctx.getExternalFilesDirs(null);
			for(File ef:dirs){
				if(ef!=null){
					paths.add(ef.getAbsolutePath());
					ctx.getSystemService(Context.STORAGE_SERVICE);
				}
			}
		}else{
			paths.add(ctx.getExternalFilesDir(null).getAbsolutePath());
		}
		paths.add(ctx.getFilesDir().getAbsolutePath());
		return paths.toArray(new String[0]);
	}

}
