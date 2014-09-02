package de.mickare.xserver;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BukkitServerThreadPool implements ServerThreadPoolExecutor {

	// Parallel running Threads(Executor) on System

	private int corePoolSize = 32;

	// Maximum Threads allowed in Pool
	private int maxPoolSize = 1024;

	// Keep alive time for waiting threads for jobs(Runnable)
	private long keepAliveTime = 30000;

	// This is the one who manages and start the work
	ThreadPoolExecutor threadPool = null;

	// Working queue for jobs (Runnable). We add them finally here
	private final ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(
			1024);

	public BukkitServerThreadPool() {
		threadPool = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
				keepAliveTime, TimeUnit.MILLISECONDS, workQueue);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.mickare.xserver.ServerThreadPoolExecutor#runTask(java.lang.Runnable)
	 */
	@Override
	public void runTask(Runnable task) {
		threadPool.execute(task);
		// System.out.println("Tasks in workQueue.." + workQueue.size());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mickare.xserver.ServerThreadPoolExecutor#shutDown()
	 */
	@Override
	public void shutDown() {
		threadPool.shutdown();
	}

}
