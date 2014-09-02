package de.mickare.xserver;

import net.md_5.bungee.api.ProxyServer;

public class BungeeServerThreadPool implements ServerThreadPoolExecutor {

	private final BungeeXServerPlugin plugin;
	
	public BungeeServerThreadPool(BungeeXServerPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void runTask(Runnable task) {
		ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
	}

	@Override
	public void shutDown() {
		// TODO Auto-generated method stub

	}

}
