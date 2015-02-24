package ubc.pavlab.morf.runnables;

import java.util.concurrent.Callable;

public class MyCallable implements Callable<String> {

	private long waitTime;
	private String name;
	private String content;

	public MyCallable(int timeInMillis, String name, String content) {
		this.waitTime = timeInMillis;
		this.name = name;
		this.content = content;
	}

	@Override
	public String call() throws Exception {
		Thread.sleep(waitTime);
		// return the thread name executing this callable task
		return Thread.currentThread().getName() + " - " + name + " - "
				+ content;
	}

}
