package partic2.pxseedloader.javase.backend.jse;

import partic2.pxseedloader.javase.backend.jse.ApiServer;
import partic2.pxseedloader.javase.backend.jse.PlatApiImpl;
import pxprpc.base.AbstractIo;
import pxprpc.base.Utils;
import pxprpc.runtimebridge.*;

import pxprpcapi.jsehelper.JseIo;
import xplatj.javaplat.partic2.util.PlatCoreConfig;


import java.io.*;
import java.nio.ByteBuffer;
import java.util.Scanner;


//Deprecated, Only for test.
public class JMain {
	static int orientation = 1;
	public static boolean[] startOptsParsed=new boolean[]{false};
	public static String selectedBackend;
	public static void ensureStartOpts(){
		synchronized (startOptsParsed){
			if(startOptsParsed[0])return;
			startOptsParsed[0]=true;
			FileInputStream in1 = null;
			try {
				in1 = new FileInputStream("data/pxseedloader-flags.txt");
				byte[] content=new byte[1024];
				int len=in1.read(content);
				String[] opts=new String(content,0,len,"utf8").split("\\s+");
				for(String opt:opts){
					if("debug".equals(opt)){
						PlatCoreConfig.debugMode=true;
					}
				}
				selectedBackend=opts[0];
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
	public static void processStartupConfig() {
		ensureStartOpts();
		try{
			JseIo.i.dataDir=new File("data").getCanonicalPath()+"/";
		}catch(IOException e){};
	}
	public static void testPxprpcServe() throws IOException {
		PipeServer serv = new PipeServer("test pipe server 1");
		serv.serve();
		while(true){
			AbstractIo conn = serv.acceptBlock();
			try{
				while(true){
					ByteBuffer[] b=new ByteBuffer[1];
					b[0]=conn.receive();
					conn.send(b);
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally {
				conn.close();
			};
		}
	}
	public static void testPxprpcClient(){
		try{
			PlatCoreConfig.get().executor.execute(()-> {
				try {
					testPxprpcServe();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			Thread.sleep(1000);
			Io io1 = Pipe.connect("test pipe server 1");
			io1.send(new ByteBuffer[]{ByteBuffer.wrap("Hello server".getBytes("utf-8"))});
			ByteBuffer[] bb = new ByteBuffer[1];
			bb[0]=io1.receive();
			System.out.println(new String(Utils.toBytes(bb[0]),"utf-8"));
			io1.send(new ByteBuffer[]{ByteBuffer.wrap("Hello client".getBytes("utf-8"))});
			bb = new ByteBuffer[1];
			bb[0]=io1.receive();
			System.out.println(new String(Utils.toBytes(bb[0]),"utf-8"));
			io1.close();

			io1 = Pipe.connect("test pipe server 1");
			io1.send(new ByteBuffer[]{ByteBuffer.wrap("Hello server".getBytes("utf-8"))});
			bb = new ByteBuffer[1];
			bb[0]=io1.receive();
			System.out.println(new String(Utils.toBytes(bb[0]),"utf-8"));
			io1.send(new ByteBuffer[]{ByteBuffer.wrap("Hello client".getBytes("utf-8"))});
			bb = new ByteBuffer[1];
			bb[0]=io1.receive();
			System.out.println(new String(Utils.toBytes(bb[0]),"utf-8"));
			io1.close();
		}catch(Exception e){
			e.printStackTrace();
		};
	}
	public static void main(String args[]) {
		PlatCoreConfig.platApi=new PlatApiImpl();
		if(PlatCoreConfig.get()==null){
			PlatCoreConfig.singleton.set(new PlatCoreConfig());
		}
		NativeHelper.loadNativeLibrary();
		ByteBuffer errorMessage = ByteBuffer.allocateDirect(255);
		NativeHelper.ensureRtbInited(errorMessage);
		if(errorMessage.get(0)!=0) {
			byte[] err = new byte[255];
			errorMessage.get(err);
			try {
				System.err.println(new String(err, "utf-8"));
			} catch (UnsupportedEncodingException e) {
			}
		}else{
			RuntimeBridgeUtils.ensureInit();
			try {
				RuntimeBridgeUtils.registerJavaPipeServer();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ApiServer.start();
		processStartupConfig();
		System.out.println("exit...");
		System.exit(0);
	}
}
