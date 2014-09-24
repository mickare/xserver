package de.mickare.xserver;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BukkitServerThreadPool extends ThreadPoolExecutor {

	// Parallel running Threads(Executor) on System
	private static int corePoolSize = 32;

	// Maximum Threads allowed in Pool
	private static int maxPoolSize = 1024;

	// Keep alive time for waiting threads for jobs(Runnable)
	private static long keepAliveTime = 30000;

	public BukkitServerThreadPool() {
		super(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1024));
	}

}
