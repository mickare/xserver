package de.mickare.xserver;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;

// public class BukkitServerThreadPool extends ThreadPoolExecutor implements ServerThreadPoolExecutor {
public class BukkitServerThreadPool implements ServerThreadPoolExecutor {
	
	// Parallel running Threads(Executor) on System
	private static int corePoolSize = 32;
	
	// Maximum Threads allowed in Pool
	private static int maxPoolSize = 1024;
	
	// Keep alive time for waiting threads for jobs(Runnable) - in seconds
	private static long keepAliveTime = 30;
	
	private final BukkitXServerPlugin bukkitPlugin;
	
	private final AtomicBoolean stopped = new AtomicBoolean( false );
	
	public BukkitServerThreadPool( BukkitXServerPlugin bukkitPlugin ) {
		// super( corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>( 1024 )
		// );
		this.bukkitPlugin = bukkitPlugin;
	}
	
	@Override
	public Future<?> runTask( Runnable task ) {
		if ( stopped.get() ) {
			return null;
		}
		FutureTask<?> f = new FutureTask<Object>( task, new Object() );
		Bukkit.getScheduler().runTaskAsynchronously( bukkitPlugin, f );
		return f;
	}
	
	@Override
	public void shutDown() {
		// this.shutdown();
		this.stopped.set( true );
	}
	
	@Override
	public Future<?> runServerTask( final Runnable task ) {
		if ( stopped.get() ) {
			return null;
		}
		FutureTask<?> f = new FutureTask<Object>( task, new Object() );
		final Thread t = new Thread( f, "XServer Server Thread" );
		t.start();
		return f;
	}
}
