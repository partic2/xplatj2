package xplatj.javaplat.partic2.util;

public interface EventHandler<S, D> {
	public void handle(S from, D data);
}
