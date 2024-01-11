package project.xplatj;

import lib.pursuer.simplewebserver.PxprpcWsServer;
import lib.pursuer.simplewebserver.XplatHTTPDServer;
import project.xplatj.backend.jse.ApiServer;
import project.xplatj.backend.jse.PlatApiImpl;
import pxprpc.base.ServerContext;
import xplatj.gdxconfig.core.PlatCoreConfig;
import xplatj.gdxplat.pursuer.utils.Env;
import xplatj.javaplat.pursuer.filesystem.FSUtils;
import xplatj.javaplat.pursuer.filesystem.impl.PrefixFS;

import xplatj.javaplat.pursuer.util.IFactory;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class JMain {
	static int orientation = 1;
	public static boolean debugMode;
	public static boolean[] startOptsParsed=new boolean[]{false};
	public static String selectedBackend;
	public static void ensureStartOpts(){
		synchronized (startOptsParsed){
			if(startOptsParsed[0])return;
			startOptsParsed[0]=true;
			FileInputStream in1 = null;
			try {
				in1 = new FileInputStream("data/xplat-flag.txt");
				byte[] content=new byte[1024];
				int len=in1.read(content);
				String[] opts=new String(content,0,len,"utf8").split("\\s+");
				for(String opt:opts){
					if("debug".equals(opt)){
						debugMode=true;
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
		try{
			PrefixFS.defaultPrefix=new File("data").getCanonicalPath()+"/";
		}catch(IOException e){};
		ensureStartOpts();
		PlatCoreConfig.platApi=new PlatApiImpl();
		if(PlatCoreConfig.get()==null){
			PlatCoreConfig.singleton.set(new PlatCoreConfig());
		}
		if("webapp".equals(selectedBackend)) {
			startWebAppBackend();
		}
	}
	public static XplatHTTPDServer httpd;
	static int httpdPort = 2080;
	public static void startWebAppBackend() {
		File cd = new File("");
		String absPath = cd.getAbsolutePath();
		File rootPath=cd;
		if(absPath.startsWith("/")) {
			//unix like system
			rootPath=new File("/");
		}else if(absPath.substring(1,3).equals(":\\") && System.getProperty("os.name").contains("Windows")){
			//windows
			rootPath=new File(absPath.substring(0,3));
		}
		if(debugMode) {
			httpd=new XplatHTTPDServer("0.0.0.0",httpdPort,rootPath);
		}else {
			httpd=new XplatHTTPDServer("127.0.0.1",httpdPort,rootPath);
		}
		try {
			PxprpcWsServer.registeredServer.put(Integer.toString(ApiServer.port), new IFactory<ServerContext>() {
				@Override
				public ServerContext create() {
					ServerContext sc=new ServerContext();
					sc.funcMap=ApiServer.tcpServ.funcMap;
					return sc;
				}
			});
			httpd.start(60*1000);
			String entryUrl = cd.getAbsoluteFile()+"/data/index.html";
			entryUrl=entryUrl.substring(rootPath.getAbsolutePath().length()).replace("\\", "/");
			entryUrl="http://127.0.0.1:"+httpdPort+"/localFile"+(entryUrl.startsWith("/")?"":"/")+entryUrl;
			System.out.println("Open url "+entryUrl+" in borwser.");
			if(System.getProperty("os.name").contains("Windows")) {
				Runtime.getRuntime().exec("explorer "+entryUrl);
			}
			System.out.println("stdin wait for coommand, input \"exit\" to exit.");
			Scanner scanin = new Scanner(System.in);
			while(true) {
				String cmd = scanin.nextLine();
				if("exit".equals(cmd)) {
					return;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void main(String args[]) {
		ApiServer.start();
		processStartupConfig();
		System.out.println("exit...");
		System.exit(0);
	}
}
