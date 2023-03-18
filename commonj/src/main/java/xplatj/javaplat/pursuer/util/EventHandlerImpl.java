package xplatj.javaplat.pursuer.util;

public class EventHandlerImpl<S, D> implements EventHandler<S, D>,Runnable {
	private S source;
	protected D data;
	public void setSource(S source) {
		this.source = source;
	}

	public S getSource() {
		return source;
	}

	public void setData(D data) {
		this.data = data;
	}

	public D getData() {
		return data;
	}

	@Override
	public void run(){
		
	}
	
	@Override
	public void handle(S from, D data) {
		setSource(from);
		setData(data);
		run();
	}
}
