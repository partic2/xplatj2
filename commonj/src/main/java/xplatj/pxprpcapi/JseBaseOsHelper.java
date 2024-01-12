package xplatj.pxprpcapi;

import pxprpc.base.Utils;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.pursuer.filesystem.FSUtils;
import xplatj.javaplat.pursuer.filesystem.IFile;
import xplatj.javaplat.pursuer.filesystem.impl.PrefixFS;
import xplatj.javaplat.pursuer.io.IDataBlock;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class JseBaseOsHelper {
    public static final String PxprpcNamespace="JseBaseOsHelper";
    public PrefixFS fs;
    public JseBaseOsHelper(){
        fs=new PrefixFS();
        fs.prefix="";
    }
    public ByteBuffer fileRead(String path, long offset, int size) throws IOException {
        IFile fo = fs.resolve(path);
        if(!fo.exists()){
            throw new IOException("File not existed");
        }else{
            IDataBlock db = fo.open();
            db.seek(offset);
            byte[] buf = new byte[size];
            int len = db.read(buf, 0, size);
            db.free();
            return ByteBuffer.wrap(buf,0,len);
        }
    }
    public void fileWrite(String path,int offset,ByteBuffer data) throws IOException {
        IFile fo = fs.resolve(path);
        if(!fo.exists()){
            fo.create();
        }else{
            IDataBlock db = fo.open();
            db.seek(offset);
            byte[] b = Utils.toBytes(data);
            db.write(b,0,b.length);
            db.free();
        }
    }
    public long fileSize(String path) throws IOException {
        IFile fo = fs.resolve(path);
        if(!fo.exists()){
            return 0;
        }else{
            IDataBlock db = fo.open();
            long r=db.size();
            db.free();
            return r;
        }
    }
    public void fileTruncate(String path,long size) throws IOException {
        IFile fo = fs.resolve(path);
        if(!fo.exists()){
            fo.create();
        }
        IDataBlock db = fo.open();
        db.resize(size);
        db.free();
    }
    public String fileList(String path) throws IOException {
        IFile fo = fs.resolve(path);
        if(!fo.exists()){
            return "";
        }else{
            return String.join("\n",fo.list());
        }
    }
    public boolean fileExists(String path){
        return fs.resolve(path).exists();
    }
    public void fileDelete(String path) throws IOException {
        new FSUtils().deleteDirectory(fs.resolve(path));
    }
    public String getDataDir(){
        return PrefixFS.defaultPrefix;
    }
}
