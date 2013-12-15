package com.mickare.xserver;

public interface ServerThreadPoolExecutor {

	/**
	 * Here we add our jobs to working queue
	 *
	 * @param task a Runnable task
	 * @return 
	 */
	public abstract boolean runTask(Runnable task);

	/**
	 * Shutdown the Threadpool if it's finished
	 */
	public abstract void shutDown();

	public abstract boolean isShutdown();

}