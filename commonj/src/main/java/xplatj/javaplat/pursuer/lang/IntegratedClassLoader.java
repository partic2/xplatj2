package xplatj.javaplat.pursuer.lang;

import java.util.Deque;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingDeque;

public class IntegratedClassLoader extends ClassLoader {
	protected Deque<ClassLoader> constituentLoaders;
	public IntegratedClassLoader(ClassLoader p) {
		super(p);
		constituentLoaders=new LinkedBlockingDeque<ClassLoader>();
	}
	
	protected Stack<String> nestedFindClass=new Stack<String>();
	protected Object findClassLock=new Object();
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		synchronized (findClassLock) {
			try{
				Class<?> cls = super.findClass(name);
				return cls;
			}catch(ClassNotFoundException e){};
			if(nestedFindClass.lastIndexOf(name)>0){
				throw new ClassNotFoundException();
			}
			
			nestedFindClass.push(name);
			for(ClassLoader loader:constituentLoaders){
				Class<?> cls=null;
				try{
					cls=loader.loadClass(name);
				}catch(ClassNotFoundException e){
					continue;
				};
				nestedFindClass.pop();
				return cls;
			}
			nestedFindClass.pop();
			throw new ClassNotFoundException("Class \""+name+"\" not found.");
		}
	}
	public void addClassLoader(ClassLoader loader){
		constituentLoaders.push(loader);
	}
	public void removeClassLoader(ClassLoader loader) {
		constituentLoaders.remove(loader);
	}
	public boolean containsClassLoader(ClassLoader loader) {
		return constituentLoaders.contains(loader);
	}
}
