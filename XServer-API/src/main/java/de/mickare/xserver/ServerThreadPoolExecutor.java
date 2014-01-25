package de.mickare.xserver;

public interface ServerThreadPoolExecutor {

	/**
	 * Here we add our jobs to working queue
	 *
	 * @param task a Runnable task
	 */
	public abstract void runTask(Runnable task);

	/**
	 * Shutdown the Threadpool if it's finished
	 */
	public abstract void shutDown();

}