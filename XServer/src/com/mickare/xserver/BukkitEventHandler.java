package com.mickare.xserver;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.mysql.jdbc.Util;

public class BukkitEventHandler extends EventHandlerObj<JavaPlugin>
{

	// Spray those tasks over some ticks...
	//private static final int SYNCTASK_SPRAY = 50;

	private final JavaPlugin plugin;

	private static final int SYNCED_EVENTS_CAPACITY = 16384;
	private static final int SYNCED_EVENTS_MAX = 2048;
	private final SyncRunHandler syncRun;
	
	// in ms
	private static final long SYNCED_EVENTS_OFFER_TIMEOUT = 20;

	protected BukkitEventHandler(JavaPlugin plugin)
	{
		super(plugin.getLogger());
		this.plugin = plugin;
		syncRun = new SyncRunHandler(plugin.getLogger());
		plugin.getServer().getScheduler().runTaskTimer(plugin, syncRun, 1, 1);
	}

	private static class SyncRunHandler implements Runnable {
		
		private final Logger logger;
		private final ArrayBlockingQueue<Runnable> pendingEvents = new ArrayBlockingQueue<Runnable>(SYNCED_EVENTS_CAPACITY, true);
		
		private SyncRunHandler(Logger logger) {
			this.logger = logger;
		}

		@Override
		public void run() {
			int counter = 0;
			while(!pendingEvents.isEmpty() && SYNCED_EVENTS_MAX > counter++) {
				try {
					pendingEvents.poll().run();
				} catch (Exception e) {
					logger.severe(e.getMessage() + "\n" + Util.stackTraceToString(e));
				}
			}
		}
		
	}
	
	
	@Override
	public void runTask(Boolean sync, XServerListenerPlugin<JavaPlugin> plugin, Runnable run)
	{
		boolean s = true;
		if (sync != null)
		{
			s = sync.booleanValue();
		}
		if (plugin.getPlugin().isEnabled())
		{

			if (s)
			{
				try {
					if(!this.syncRun.pendingEvents.offer(run, SYNCED_EVENTS_OFFER_TIMEOUT, TimeUnit.MILLISECONDS)) {
						plugin.getPlugin().getLogger().warning("Event chain is full and event processing timed out!\n");
					}
				} catch (InterruptedException e) {
					plugin.getPlugin().getLogger().warning("Event Interrupt Exception " + e.getMessage());
				}
			} else
			{
				//plugin.getPlugin().getServer().getScheduler().runTaskAsynchronously(plugin.getPlugin(), run);
				run.run();
			}
		}
	}

	public JavaPlugin getPlugin()
	{
		return plugin;
	}

	@Override
	public void registerListener(JavaPlugin plugin, XServerListener lis)
	{
		XServerListenerPlugin<JavaPlugin> lp = null;
		lp = this.getListPlugin(plugin);
		if (lp == null)
		{
			lp = new BukkitListenerPlugin(plugin);
		}
		this.registerListener(lp, lis);
	}

}
