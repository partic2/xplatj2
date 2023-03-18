package xplatj.javaplat.pursuer.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class NativeInvocationHandler implements InvocationHandler {

	@Override
	public native Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable;

}
