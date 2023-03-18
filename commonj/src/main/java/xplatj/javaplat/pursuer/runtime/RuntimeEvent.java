package xplatj.javaplat.pursuer.runtime;

import xplatj.javaplat.pursuer.util.Container;
import xplatj.javaplat.pursuer.util.EventHandler;

public class RuntimeEvent {
	static{
		getDefault().startCall();
		Runtime.getRuntime().addShutdownHook(new ShutdownHookThread());
	}
	private static class ShutdownHookThread extends Thread{
		@Override
		public void run() {
			getDefault().quitCall();
			super.run();
		}
	}
	
	public static RuntimeEvent instance;
	public static RuntimeEvent getDefault(){
		if(instance==null){
			instance=new RuntimeEvent();
		}
		return instance;
	}
	public Container<EventHandler<RuntimeEvent, Integer>> onStart;
	public Container<EventHandler<RuntimeEvent, Integer>> onQuit;
	public RuntimeEvent() {
		onStart=new Container<EventHandler<RuntimeEvent,Integer>>();
		onQuit=new Container<EventHandler<RuntimeEvent,Integer>>();
	}
	public void startCall(){
		if(onStart.get()!=null){
			onStart.get().handle(this, 1);
		}
	}
	public void quitCall(){
		if(onStart.get()!=null){
			onStart.get().handle(this, 1);
		}
	}
}
