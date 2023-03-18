package xplatj.javaplat.pursuer.util;

import java.util.*;

public abstract class EventListener2<S, D> extends EventHandlerImpl<S, D> {
	
	public static <S,E> void listen(Container<EventHandler<S,E>> container,final EventHandler<S, E> handler) {
		new EventListener2<S, E>(container) {
			@Override
			public void run() {
				handler.handle(getSource(),getData());
				super.run();
			}
		};
	}
	
	private LinkedList<HandlerGroup<S, D>> listening;

	public EventListener2() {
		listening = new LinkedList<HandlerGroup<S, D>>();
	};

	public EventListener2(Container<EventHandler<S, D>> handler) {
		this();
		listen(handler);
	}

	public void listen(Container<EventHandler<S, D>> handler) {
		if (handler.get() == null) {
			handler.set(new HandlerGroup<S, D>());
		}
		if (handler.get() instanceof HandlerGroup) {
			HandlerGroup<S, D> grp = (HandlerGroup<S, D>) handler.get();
			grp.getHandlers().add(this);
			listening.add(grp);
		} else {
			HandlerGroup<S, D> grp = new HandlerGroup<S, D>();
			grp.getHandlers().add(handler.get());
			grp.getHandlers().add(this);
			handler.set(grp);
			listening.add(grp);
		}
	}

	public void cancelAllListen() {
		Iterator<HandlerGroup<S, D>> ithg = listening.iterator();
		while (ithg.hasNext()) {
			HandlerGroup<S, D> ehg = ithg.next();
			ehg.getHandlers().remove(this);
		}
		listening.clear();
	}

	public void cancelListen(HandlerGroup<S, D> h) {
		h.getHandlers().remove(this);
		listening.remove(h);
	}
}
