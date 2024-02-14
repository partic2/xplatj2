package pxprpcapi.jsehelper;

import pxprpc.base.Serializer2;
import pxprpc.base.Utils;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.pursuer.filesystem.FSUtils;
import xplatj.javaplat.pursuer.filesystem.impl.PrefixFS;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class JseIo {
    public static final String PxprpcNamespace="JseHelper.JseIo";
    public PrefixFS fs;
    public JseIo(){
        fs=new PrefixFS();
        fs.prefix="";
    }
    public String realpath(String path) throws IOException {
        return new File(path).getCanonicalPath();
    }
    public void unlink(String path) throws IOException {
        if(!new File(path).delete()){
            throw new IOException("deleted failed");
        }
    }
    public void rename(String path,String newPath) throws IOException {
        if(!new File(path).renameTo(new File(newPath))){
            throw new IOException("rename failed");
        };
    }
    //File handler
    public static class FH implements Closeable{
        File f;
        FileChannel fc;
        @Override
        public void close() throws IOException {
            if(fc!=null)fc.close();
        }
    }
    //return fileHandler,path
    @MethodTypeDecl("s->os")
    public Object[] mkstemp(String template) throws IOException {
        String prefix="";
        for(int i=template.length();i>=0;i--){
            if(template.charAt(i)!='X'){
                prefix=template.substring(0,i);
            }
        }
        FH fh = new FH();
        fh.f=File.createTempFile(prefix,"");
        fh.fc=FileChannel.open(Paths.get(fh.f.getCanonicalPath()));
        return new Object[]{fh,fh.f.getCanonicalPath()};
    }

    public ByteBuffer fhRead(FH f,long offset,int length) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(length);
        f.fc.read(buf,offset);
        Utils.flip(buf);
        return buf;
    }
    public int fhWrite(FH f,long offset,ByteBuffer buf) throws IOException {
        return f.fc.write(buf);
    }
    public void fhClose(FH f) throws IOException {
        f.close();
    }
    public void fhTruncate(FH f,long offset) throws IOException {
        f.fc.truncate(offset);
    }
    //return isFile,isDirectory,size,modifyTimeStampInSecond
    @MethodTypeDecl("s->ccll")
    public Object[] stat(String path) throws IOException {
        File f=new File(path);
        return new Object[]{f.isFile(),f.isDirectory(),f.length(),f.lastModified()};
    }
    public FH open(String path,String flag,int mode) throws IOException {
        Set<OpenOption> openModeflag=new HashSet<>();
        if(flag.contains("r")){
            openModeflag.add(StandardOpenOption.READ);
        }
        if(flag.contains("+")){
            openModeflag.add(StandardOpenOption.READ);
            openModeflag.add(StandardOpenOption.WRITE);
        }
        if(flag.contains("w")){
            openModeflag.add(StandardOpenOption.CREATE);
            openModeflag.add(StandardOpenOption.WRITE);
        }
        FH fh = new FH();
        fh.f=new File(path);
        if(!(fh.f.exists() && fh.f.isDirectory())){
            fh.fc=FileChannel.open(Paths.get(path),openModeflag);
            if(flag.contains("w")){
                fh.fc.truncate(0);
            }
        }
        return fh;
    }
    public void rmdir(String path) throws IOException {
        if(!new File(path).delete()){
            throw new IOException("rmdir failed");
        }
    }
    public void mkdir(String path) throws IOException {
        if(!new File(path).mkdirs()){
            throw new IOException("mkdir failed");
        }
    }

    public void copyFile(String path,String newPath) throws IOException {
        Files.copy(Paths.get(path),Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);
    }


    public ByteBuffer readdir(String path) throws IOException{
        TableSerializer ser = new TableSerializer().setHeader2(null, new String[]{"name", "isDirectory", "isFile"});
        for(File child:new File(path).listFiles()){
            ser.addRow(new Object[]{child.getName(),child.isFile(),child.isDirectory()});
        }
        return ser.build();
    }

    public void rm(String path) throws IOException {
        Stack<File> delStack = new Stack<File>();
        delStack.push(new File(path));
        int i=0;
        for(i=0;!delStack.empty() && i<1000000;i++){
            File top = delStack.peek();
            if(top.isDirectory()){
                File[] children = top.listFiles();
                if(children.length==0){
                    unlink(top.getCanonicalPath());
                    delStack.pop();
                }else{
                    for(File child:top.listFiles()){
                        delStack.push(child);
                    }
                }
            }else{
                unlink(top.getCanonicalPath());
                delStack.pop();
            }
        }
        if(i==1000000){
            throw new IOException("recursive too much");
        }
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

    public String getProp(String prop){
        String result = System.getProperty(prop);
        if(result==null){
            //How to handle prop not exists?
            return "";
        }else{
            return result;
        }
    }
    public String dumpPropNames(){
        return Utils.stringJoin("\n",System.getProperties().stringPropertyNames());
    }
}
