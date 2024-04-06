package xplatj.javaplat.partic2.util;

import java.util.ArrayList;
import java.util.LinkedList;

public class AsyncFuncChain{
	public interface Controler{
		void next();
		void throw2(Exception excep);
	}

	protected Controler ctl=new Controler() {
		@Override
		public void next() {
			nextCb++;
			AsyncFuncChain.this.step();
		}

		@Override
		public void throw2(Exception excep) {
			AsyncFuncChain.this.excep=excep;
			AsyncFuncChain.this.step();
		}

	};

	protected ArrayList<OneArgRunnable<Controler>> funcChain=new ArrayList<OneArgRunnable<Controler>>();
	public Exception excep;
	public AsyncFuncChain() {
	}
	public int nextCb=0;
	public void step(){
		if(excep!=null){
			if(errorHandler!=null){
				errorHandler.run(excep);
			}else{
				excep.printStackTrace();
			}
			return;
		}
		try{
			if(nextCb<funcChain.size()){
				funcChain.get(nextCb).run(ctl);
			}
		}catch(Exception e){
			AsyncFuncChain.this.excep=e;
			step();
		}
	}
	public AsyncFuncChain then(OneArgRunnable<Controler> cb){
		funcChain.add(cb);
		return this;
	}
	public OneArgRunnable<Exception> errorHandler;
	public AsyncFuncChain catch2(OneArgRunnable<Exception> cb){
		this.errorHandler=errorHandler;
		return this;
	}

}
