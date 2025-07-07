package xplatj.gdxconfig.core;

import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import xplatj.javaplat.partic2.filesystem.FSUtils;
import xplatj.javaplat.partic2.filesystem.IFile;
import xplatj.javaplat.partic2.filesystem.IFileSystem;
import xplatj.javaplat.partic2.filesystem.impl.*;
import xplatj.javaplat.partic2.lang.IntegratedClassLoader;
import xplatj.javaplat.partic2.net.DefaultDatagramSocketFactory;
import xplatj.javaplat.partic2.net.DefaultServerSocketFactory;
import xplatj.javaplat.partic2.net.DefaultSocketFactory;
import xplatj.javaplat.partic2.net.ICommonNet;
import xplatj.javaplat.partic2.net.CommonNetDefaultImpl;
import xplatj.javaplat.partic2.util.Container;
import xplatj.javaplat.partic2.util.IFactory;
import xplatj.javaplat.partic2.util.ScheduledCustomThreadPoolExecutor;
import xplatj.javaplat.partic2.util.VariableTable;
import xplatj.platform.PlatApi;

public class PlatCoreConfig implements Closeable {
	public static boolean debugMode=false;
	public static Container<PlatCoreConfig> singleton = new Container<PlatCoreConfig>();
	public static PlatCoreConfig get() {
		return singleton.get();
	}

	public static Object releaseSingletonLock = new Object();

	public static void releaseCurrent() {
		synchronized (singleton) {
			PlatCoreConfig s = singleton.get();
			if (s != null) {
				try {
					s.close();
				} finally {
					singleton.set(null);
				}
			}
		}
	}

	public IFileSystem fs;
	public String os;
	//remove in future?
	protected ICommonNet commonNet;
	public ICommonNet getNetManager(){
		if(commonNet==null){
			IFile netCfgFile = fs.resolve("config/net.ini");

			if (netCfgFile.exists()) {
				CommonNetDefaultImpl.Config netCfg = new CommonNetDefaultImpl.Config();
				Properties props = new Properties();
				InputStream input = null;
				try {
					input = new FSUtils().readFromIFile(netCfgFile);
					props.load(input);
					input.close();
					String prop = props.getProperty("NetManager.msgPort");
					if (prop != null) {
						netCfg.msgPort = Integer.parseInt(prop);
					}
					prop = props.getProperty("NetManager.streamPort");
					prop = props.getProperty("NetManager.othersMsgPort");

					prop = props.getProperty("NetManager.othersStreamPort");
					prop = props.getProperty("NetManager.ipv4");
					if (prop != null) {
						netCfg.ipv4 = Boolean.parseBoolean(prop);
					}
					prop = props.getProperty("NetManager.ipv6");
					if (prop != null) {
						netCfg.ipv6 = Boolean.parseBoolean(prop);
					}
					prop = props.getProperty("NetManager.filterSelf");
					if (prop != null) {
						netCfg.filterSelf = Boolean.parseBoolean(prop);
					}
				} catch (IOException e) {
					e.printStackTrace();
					try {
						input.close();
					} catch (IOException e1) {
					}
				}
				commonNet = new CommonNetDefaultImpl(executor, netCfg);
			} else {
				commonNet = new CommonNetDefaultImpl(executor);
			}
		}
		return this.commonNet;
	}
	public InputStream stdin;
	public OutputStream stdout;
	public OutputStream stderr;
	public ScheduledExecutorService executor;
	public Reader stdreader;
	public PrintWriter stdwriter;
	public Collection<Runnable> runOnClosing = new LinkedList<Runnable>();
	public VariableTable context;
	public IFactory<ServerSocket> serverSocketFactory;
	public IFactory<Socket> socketFactory;
	public IFactory<DatagramSocket> datagramSocketFactory;
	public IntegratedClassLoader classSpace;
	//need be set by backend, if use
	public static PlatApi platApi;
	
	public boolean initDefault() {
		System.setProperty("line.separator", "\n");
		context = new VariableTable(null);
		if (os == null) {
			File tf = new File("/sdcard");
			if (tf.exists()) {
				os = "Android";
			} else {
				String osName = System.getProperty("os.name").toLowerCase();
				if (osName.startsWith("windows")) {
					os = "Windows";
				} else {
					os = "Linux";
				}
			}
		}
		String jvmName = System.getProperty("java.vm.name");
		RejectedExecutionHandler defaultRejectedHandler = new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				executor.setCorePoolSize(executor.getCorePoolSize() + 10);
			}
		};
		if (jvmName.equals("Dalvik")) {
			String jvmVersion = System.getProperty("java.vm.version");
			int majorVersion = Integer.parseInt(jvmVersion.substring(0, jvmVersion.indexOf('.')));

			if (majorVersion < 2) {
				executor = new ScheduledCustomThreadPoolExecutor(Executors.newCachedThreadPool(new ThreadFactory() {
					int threadId = 0;

					@Override
					public Thread newThread(Runnable r) {
						Thread th = new Thread(null, r, "threadId-" + threadId, 10240 * 1024);
						th.setDaemon(true);
						return th;
					}
				}));
			} else {
				executor = new ScheduledCustomThreadPoolExecutor(Executors.newCachedThreadPool());
			}
		} else {
			executor = new ScheduledCustomThreadPoolExecutor(Executors.newCachedThreadPool());
		}

		System.getProperties().put("file.encoding", "utf-8");
		PrefixFS pfs = new PrefixFS();
		fs = pfs;
		stdin = System.in;
		stdout = System.out;
		stderr = System.err;

		stdwriter = new PrintWriter(new OutputStreamWriter(stdout));
		stdreader = new InputStreamReader(stdin);

		socketFactory = new DefaultSocketFactory();
		serverSocketFactory = new DefaultServerSocketFactory();
		datagramSocketFactory = new DefaultDatagramSocketFactory();
		classSpace = new IntegratedClassLoader(getClass().getClassLoader());
		return true;
	}

	public PlatCoreConfig() {
		this(true);
	}

	public PlatCoreConfig(boolean initDefault) {
		if (initDefault) {
			initDefault();
		}
	}

	public ClassLoader loadClasses(File[] cp) {
		ClassLoader loader = platApi.load(cp, classSpace);
		classSpace.addClassLoader(loader);
		return loader;
	}

	public void unloadClasses(ClassLoader c) {
		classSpace.removeClassLoader(c);
	}

	@Override
	public void close() {
		close2(true);
	}

	public void close2(final boolean waitAllTask) {
		for (Runnable er : runOnClosing) {
			er.run();
		}
		try {
			if (context != null) {
				context.close();
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		try {
			if (commonNet != null) {
				commonNet.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (executor != null) {
			executor.shutdownNow();
			if (waitAllTask&&!Thread.currentThread().isInterrupted()) {
				try {
					executor.awaitTermination(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
