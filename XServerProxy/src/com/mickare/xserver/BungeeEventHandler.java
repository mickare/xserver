package com.mickare.xserver;

import net.md_5.bungee.api.plugin.Plugin;

public class BungeeEventHandler extends EventHandler<Plugin> {

	private final Plugin plugin;

	protected BungeeEventHandler(Plugin plugin) {
		super(plugin.getLogger());
		this.plugin = plugin;
	}

	@Override
	protected void runTask(Boolean sync, XServerListenerPlugin<Plugin> plugin, Runnable run) {
		plugin.getPlugin().getProxy().getScheduler().runAsync(plugin.getPlugin(), run);
	}

	public Plugin getPlugin() {
		return plugin;
	}

	@Override
	public void registerListener(Plugin plugin, XServerListener lis) {
		XServerListenerPlugin<Plugin> lp = null;
		lp = this.getListPlugin(plugin);
		if(lp == null) {
			lp = new BungeeListenerPlugin(plugin);
		}
		this.registerListener(lp, lis);
	}


}
