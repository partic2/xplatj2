package xplatj.javaplat.pursuer.util;


public class IntervalCounter implements Runnable {
	Integer count;
	Integer step;
	EventHandler<IntervalCounter, Integer> ih;
	Integer trigPoint;

	public IntervalCounter() {
		count = 0;
		step = 1;
	}

	public void setTrigger(Integer trigPoint, EventHandler<IntervalCounter, Integer> callback) {
		ih = callback;
		this.trigPoint = trigPoint;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public int getStep() {
		return step;
	}

	public void run() {
		count += step;
		if (ih != null) {
			if (count == trigPoint) {
				ih.handle(this, 1);
			}
		}

	}

	public int getCount() {
		return count;
	}

	public void setCount(int c) {
		count = c;
	}
}
