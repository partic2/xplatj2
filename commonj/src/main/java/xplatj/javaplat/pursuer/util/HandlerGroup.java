package xplatj.javaplat.pursuer.util;

import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class HandlerGroup<S, D> extends EventHandlerImpl<S, D> {
	private LinkedList<EventHandler<S, D>> handlers;

	public HandlerGroup() {
		handlers = new LinkedList<EventHandler<S, D>>();
	}

	public Collection<EventHandler<S, D>> getHandlers() {
		return handlers;
	}

	@Override
	public void run() {
		@SuppressWarnings("unchecked")
		Iterator<EventHandler<S, D>> iteh = ((Iterable<EventHandler<S, D>>) handlers.clone()).iterator();
		while (iteh.hasNext()) {
			EventHandler<S, D> eeh = iteh.next();
			eeh.handle(getSource(), getData());
		}
	}
}
