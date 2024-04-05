package xplatj.javaplat.partic2.util;

import java.util.ArrayList;
import java.util.LinkedList;

public class AsyncFuncChain{
	public interface Controler{
		void next();
		void throw2(Exception excep);
	}
	public interface Cb{
		void run(Controler ctl);
	}

	protected ArrayList<Cb> funcChain=new ArrayList<Cb>();
	public Exception excep;
	public AsyncFuncChain() {
	}
	public int nextCb=0;
	public void step(){
		if(excep!=null){
			if(errorHandler!=null){
				errorHandler.call(excep);
			}else{
				excep.printStackTrace();
			}
			return;
		}
		try{
			funcChain.get(nextCb).run(new Controler() {
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
			});
		}catch(Exception e){
			AsyncFuncChain.this.excep=e;
			step();
		}
	}
	public AsyncFuncChain then(Cb cb){
		funcChain.add(cb);
		return this;
	}
	public OneArgFunc<Boolean,Exception> errorHandler;
	public AsyncFuncChain catch2(OneArgFunc<Boolean,Exception> errorHandler){
		this.errorHandler=errorHandler;
		return this;
	}

}
