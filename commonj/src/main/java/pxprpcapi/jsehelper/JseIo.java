package pxprpcapi.jsehelper;

import pxprpc.base.Utils;
import pxprpc.extend.AsyncReturn;
import pxprpc.extend.MethodTypeDecl;
import pxprpc.extend.TableSerializer;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.javaplat.partic2.filesystem.impl.PrefixFS;
import xplatj.javaplat.partic2.io.stream.StreamTransmit;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.Stack;

public class JseIo implements Closeable{
    public static JseIo i;
    public static final String PxprpcNamespace="JseHelper.JseIo";
    public PrefixFS fs;
    public JseIo(){
        fs=new PrefixFS();
        fs.prefix="";
        i=this;
    }
    public String realpath(String path) throws IOException {
        return new File(path).getCanonicalPath();
    }
    public void unlink(String path) throws IOException {
        new File(path).delete();
    }
    public void rename(String path,String newPath) throws IOException {
        new File(path).renameTo(new File(newPath));
    }

    @Override
    public void close() throws IOException {
        if(i==this)i=null;
    }

    //File handler
    public static class FH implements Closeable {
        File f;
        RandomAccessFile raf;
        @Override
        public void close() throws IOException {
            if(raf!=null)raf.close();
        }
    }
    //return fileHandler,path
    @MethodTypeDecl("s->os")
    public Object[] mkstemp(String template) throws IOException {
        String prefix="";
        for(int i=template.length();i>=0;i--){
            if(template.charAt(i)!='X'){
                prefix=template.substring(0,i);
				break;
            }
        }
        FH fh = new FH();
        fh.f=File.createTempFile(prefix,"");
        fh.raf=new RandomAccessFile(fh.f.getCanonicalPath(),"rw");
        return new Object[]{fh,fh.f.getCanonicalPath()};
    }
    public ByteBuffer fhRead(FH f,long offset,int length) throws IOException {
        f.raf.seek(offset);
        ByteBuffer buf = ByteBuffer.allocate(length);
        int byteRead=f.raf.read(buf.array());
        if(byteRead<0)byteRead=0;
        Utils.setLimit(buf,byteRead);
        return buf;
    }
    public int fhWrite(FH f,long offset,ByteBuffer buf) throws IOException {
        f.raf.seek(offset);
        //TODO: supprt native buffer.
        f.raf.write(buf.array(),buf.position(),buf.remaining());
        return buf.remaining();
    }
    public void fhClose(FH f) throws IOException {
        f.close();
    }
    public void fhTruncate(FH f,long offset) throws IOException {
        f.raf.setLength(offset);
    }
    //return type:dir|file,size,modifyTimeStampInSecond
    @MethodTypeDecl("s->sll")
    public Object[] stat(String path) throws IOException {
        File f=new File(path);
        String type="";
        if(!f.exists()){
            throw new IOException("File not exists");
        }
        if(f.isFile()){
            type="file";
        }else if(f.isDirectory()){
            type="dir";
        }
        return new Object[]{type,f.length(),f.lastModified()};
    }
    public FH open(String path,String flag,int mode) throws IOException {
        String openMode="r";
        boolean create=false;
        if(flag.contains("+")){
            openMode="rw";
        }
        if(flag.contains("w")){
            openMode="rw";
        }
        FH fh = new FH();
        fh.f=new File(path);
        if(!(fh.f.exists() && fh.f.isDirectory())){
            fh.raf=new RandomAccessFile(fh.f.getCanonicalPath(),openMode);
            if(flag.contains("w")){
                fh.raf.setLength(0);
            }
        }
        return fh;
    }
    public void rmdir(String path) throws IOException {
        new File(path).delete();
    }
    public void mkdir(String path) throws IOException {
        new File(path).mkdirs();
    }
    protected void copyFileRecursively(String path,String newPath) throws IOException {
        File f1 = new File(path);
        for(File child : f1.listFiles()){
            File dstChild=new File(newPath+child.getName());
            if(child.isDirectory()){
                if(dstChild.isFile()){
                    dstChild.delete();
                }
                copyFileRecursively(child.getPath(),newPath+child.getName());
            }else{
                if(dstChild.isDirectory()){
                    rm(dstChild.getPath());
                }
                new StreamTransmit().setAutoCloseStream(true).start(null,
                        new FileInputStream(child),new FileOutputStream(dstChild),
                        16*1024*1024,4096,null);
            }
        }
    }
    public void copyFile(String path,String newPath) throws IOException {
        copyFileRecursively(path,newPath);
    }

    public ByteBuffer readdir(String path) throws IOException{
        //return type:dir|file,size,modifyTimeStampInSeconds
        TableSerializer ser = new TableSerializer().setColumnsInfo2(null, new String[]{"name","type","size","mtime"});
        File[] children = new File(path).listFiles();
        if(children==null){
            throw new IOException("File not exists");
        }
        for(File child:children){
            try{
                ser.addRow(new Object[]{child.getName(),child.isFile()?"file":"dir",child.isFile()?child.length():0,child.lastModified()});
            }catch(Exception e){}
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
        try{
            proc.exitValue();
            return false;
        }catch(Exception e){
            return true;
        }
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

    public Socket tcpConnect(AsyncReturn<Socket> aret,String host, int port) throws IOException {
        PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                Socket soc = new Socket();
                try{
                    soc.connect(new InetSocketAddress(InetAddress.getByName(host),port));
                    aret.resolve(soc);
                }catch (Exception ex){
                    try {
                        soc.close();
                    } catch (IOException e) {}
                    aret.reject(ex);
                }
            }
        });
        return null;
    }
    //return input stream,output stream
    @MethodTypeDecl("o->oo")
    public Object[] tcpStreams(Socket soc) throws IOException {
        InputStream input = soc.getInputStream();
        OutputStream output = soc.getOutputStream();
        return new Object[]{input,output};
    }

    public ServerSocket tcpListen(String host,int port) throws IOException{
        ServerSocket ss=new ServerSocket(port,8,InetAddress.getByName(host));
        return ss;
    }
    public Socket tcpAccept(AsyncReturn<Socket> aret,ServerSocket ss) throws IOException {
        PlatCoreConfig.get().executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    aret.resolve(ss.accept());
                } catch (IOException e) {
                    aret.reject(e);
                }
            }
        });
        return null;
    }
    public String platform(){
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.contains("windows")){
            return "windows";
        }else if(osName.contains("linux")){
            return "linux";
        }else if(osName.contains("macos")){
            return "darwin";
        }else{
            return "";
        }
    }
}
