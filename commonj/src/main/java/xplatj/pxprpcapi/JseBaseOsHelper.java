package xplatj.pxprpcapi;

import pxprpc.base.Utils;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.BuiltInFuncList;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.pursuer.filesystem.FSUtils;
import xplatj.javaplat.pursuer.filesystem.IFile;
import xplatj.javaplat.pursuer.filesystem.impl.PrefixFS;
import xplatj.javaplat.pursuer.io.IDataBlock;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class JseBaseOsHelper {
    public static final String PxprpcNamespace="JseBaseOsHelper";
    public PrefixFS fs;
    public JseBaseOsHelper(){
        fs=new PrefixFS();
        fs.prefix="";
    }

    public RpcFile fileOpen(String path) throws IOException {
        RpcFile fd = new RpcFile();
        fd.fi=fs.resolve(path);
        if(fd.fi.canOpen()) {
            fd.db=fd.fi.open();
        }
        return fd;
    }
    public void fileSeek(RpcFile fd,long pos) throws IOException {
        fd.db.seek(pos);
    }
    public ByteBuffer fileRead(RpcFile fd,int len) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(len);
        fd.db.read(buf.array(),0,len);
        return buf;
    }
    public int fileWrite(RpcFile fd,ByteBuffer buf) throws IOException {
        return fd.db.write(buf.array(),buf.position(),buf.remaining());
    }
    public long fileSize(RpcFile fd) throws IOException {
        return fd.db.size();
    }
    public void fileTruncate(RpcFile fd,long size) throws IOException {
        fd.db.resize(size);
    }
    public ByteBuffer fileList(RpcFile fd) throws IOException {
        TableSerializer ser = new TableSerializer();
        ser.setHeader("sc",new String[]{"name","isDir"});
        for(String child:fd.fi.list()){
            ser.addRow(new Object[]{child,fd.fi.next(child).list()!=null});
        }
        return ser.build();
    }
    public boolean fileExists(RpcFile fd){
        return fd.fi.exists();
    }
    public void fileDelete(RpcFile fd) throws IOException {
        new FSUtils().deleteDirectory(fd.fi);
    }
    public Process execCommand(String command) throws IOException {
        return Runtime.getRuntime().exec(command);
    }
    public int processWait(final AsyncReturn<Integer> ret,final Process proc){
        PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ret.resolve(proc.waitFor());
                } catch (InterruptedException e) {
                    ret.reject(e);
                }
            }
        });
        return 0;
    }
    public boolean processIsAlive(Process proc){
        return proc.isAlive();
    }
    @MethodTypeDecl("occc->ooo")
    public Object[] processStdio(Process proc,boolean in,boolean out,boolean err){
        OutputStream stdin=null;
        InputStream stdout=null,stderr=null;
        if(in)stdin=proc.getOutputStream();
        if(out)stdout=proc.getInputStream();
        if(err)stderr=proc.getErrorStream();
        return new Object[]{stdin,stdout,stderr};
    }
    public ByteBuffer inputRead(final AsyncReturn<ByteBuffer> ret,final InputStream in, final int len){
        PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ByteBuffer buf = ByteBuffer.allocate(len);
                    int rlen=in.read(buf.array());
                    if(rlen<0){
                        Utils.setLimit(buf,0);
                    }else{
                        Utils.setLimit(buf,rlen);
                    }
                    ret.resolve(buf);
                } catch (IOException e) {
                    ret.reject(e);
                }
            }
        });
        return null;
    }
    public void outputWrite(OutputStream out, ByteBuffer buf) throws IOException {
        out.write(buf.array(),buf.position(),buf.remaining());
        out.flush();
    }
    public String getDataDir(){
        return PrefixFS.defaultPrefix;
    }
}
