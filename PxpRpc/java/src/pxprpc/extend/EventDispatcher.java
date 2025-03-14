package pxprpc.extend;

import pxprpc.base.PxpRequest;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class EventDispatcher extends CommonCallable {
	public EventDispatcher() {
		this.tResult=new char[]{'o'};
	}
	public EventDispatcher setEventType(Class<?> c){
		this.tResult=new char[]{TypeDeclParser.jtypeToValueInfo(c)};
		return this;
	}
	protected Queue<PxpRequest> receivers = new LinkedList<PxpRequest>();

	public void fireEvent(Object evt) {
		synchronized (receivers){
			PxpRequest r;
			for(r = receivers.poll();r!=null;r=receivers.poll()){
				writeResult(r,evt);
				r.done();
			}
		}
	}

	public void clearReceivers() {
		receivers.clear();
	}

	@Override
	public void call(PxpRequest req) throws IOException {
		synchronized (receivers) {
			receivers.offer(req);
		}
	}
}
