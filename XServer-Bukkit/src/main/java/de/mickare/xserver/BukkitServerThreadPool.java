package de.mickare.xserver;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BukkitServerThreadPool extends ThreadPoolExecutor implements ServerThreadPoolExecutor {
	
	// Parallel running Threads(Executor) on System
	private static int corePoolSize = 32;
	
	// Maximum Threads allowed in Pool
	private static int maxPoolSize = 1024;
	
	// Keep alive time for waiting threads for jobs(Runnable) - in seconds
	private static long keepAliveTime = 30;
	
	public BukkitServerThreadPool( BukkitXServerPlugin bukkitPlugin ) {
		super( corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>( 1024 ) );
	}
	
	@Override
	public Future<?> runTask( Runnable task ) {
		return super.submit( task );
	}
	
	@Override
	public void shutDown() {
		this.shutdown();
	}
	
	@Override
	public Future<?> runServerTask( final Runnable task ) {
		FutureTask<?> f = new FutureTask<Object>( task, new Object() );
		final Thread t = new Thread( f, "XServer Server Thread" );
		t.start();
		return f;
	}
}
