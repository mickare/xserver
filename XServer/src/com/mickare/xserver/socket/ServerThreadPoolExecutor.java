package com.mickare.xserver.socket;

import java.util.concurrent.*;

public class ServerThreadPoolExecutor {

	//Parallel running Threads(Executor) on System
	private int corePoolSize = 2;
 
    //Maximum Threads allowed in Pool
    private int maxPoolSize = 8;
 
    //Keep alive time for waiting threads for jobs(Runnable)
    private long keepAliveTime = 10;
 
    //This is the one who manages and start the work
    ThreadPoolExecutor threadPool = null;
 
    //Working queue for jobs (Runnable). We add them finally here
	private final ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(50);

    public ServerThreadPoolExecutor() {
        threadPool = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                keepAliveTime, TimeUnit.SECONDS, workQueue);
    }
    

    /**
     * Here we add our jobs to working queue
     *
     * @param task a Runnable task
     */
    public void runTask(Runnable task) { 
        threadPool.execute(task);
        //System.out.println("Tasks in workQueue.." + workQueue.size());
    }
 
    /**
     * Shutdown the Threadpool if it's finished
     */
    public void shutDown() {
        threadPool.shutdown();
    }
    
}
