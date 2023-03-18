package xplatj.javaplat.pursuer.util;

public interface EventHandler<S, D> {
	public void handle(S from, D data);
}
