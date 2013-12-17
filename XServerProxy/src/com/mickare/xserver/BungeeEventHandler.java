package com.mickare.xserver;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.mysql.jdbc.Util;

import net.md_5.bungee.api.plugin.Plugin;

public class BungeeEventHandler extends EventHandlerObj<Plugin> {

	private final BungeeXServerPlugin plugin;

	private final RunHandler runhandler;

	private final static int SYNCED_EVENTS_CAPACITY = 1024;
	private final static int SYNCED_EVENTS_MAX = 128;

	protected BungeeEventHandler(BungeeXServerPlugin plugin) {
		super(plugin.getLogger());
		this.plugin = plugin;
		this.runhandler = new RunHandler(plugin.getLogger());
		plugin.getProxy().getScheduler().runAsync(plugin, runhandler);
	}

	private static class RunHandler implements Runnable {

		private final Logger logger;
		private final ArrayBlockingQueue<Runnable> pendingEvents = new ArrayBlockingQueue<Runnable>(
				SYNCED_EVENTS_CAPACITY, true);

		private RunHandler(Logger logger) {
			this.logger = logger;
		}

		@Override
		public void run() {
			try {
				int counter = 0;
				while (true) {
					try {
						pendingEvents.take().run();
					} catch (Exception e) {
						logger.severe(e.getMessage() + "\n"
								+ Util.stackTraceToString(e));
					}
					if (SYNCED_EVENTS_MAX < counter) {
						Thread.sleep(50);
						counter = 0;
					}
				}
			} catch (InterruptedException e) {

			}
		}
	}

	@Override
	public void runTask(Boolean sync, XServerListenerPlugin<Plugin> plugin,
			Runnable run) {
		// plugin.getPlugin().getProxy().getScheduler().runAsync(plugin.getPlugin(),
		// run);
		// this.plugin.getManager().getThreadPool().runTask(run);

		try {
			this.runhandler.pendingEvents.offer(run, 10, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {

		}
		
	}

	public Plugin getPlugin() {
		return plugin;
	}

	@Override
	public void registerListener(Plugin plugin, XServerListener lis) {
		XServerListenerPlugin<Plugin> lp = null;
		lp = this.getListPlugin(plugin);
		if (lp == null) {
			lp = new BungeeListenerPlugin(plugin);
		}
		this.registerListener(lp, lis);
	}

}
