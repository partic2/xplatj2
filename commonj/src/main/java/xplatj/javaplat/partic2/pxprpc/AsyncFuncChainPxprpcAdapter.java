package xplatj.javaplat.partic2.pxprpc;

import pxprpc.base.PxpRequest;
import pxprpc.extend.AsyncReturn;
import xplatj.javaplat.partic2.util.AsyncFuncChain;

public class AsyncFuncChainPxprpcAdapter<T> implements AsyncReturn<T> {
    T[] setResult;
    AsyncFuncChain.Controler ctl;
    public AsyncFuncChainPxprpcAdapter(T[] setResult, AsyncFuncChain.Controler ctl){
        this.setResult=setResult;
        this.ctl=ctl;
    }
    @Override
    public void resolve(T r) {
        if(setResult!=null){
            setResult[0]=r;
        }
        ctl.next();
    }

    @Override
    public void reject(Exception ex) {
        ctl.throw2(ex);
    }

    @Override
    public PxpRequest getRequest() {
        return null;
    }
}
