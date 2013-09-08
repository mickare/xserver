package com.mickare.xserver;

import net.md_5.bungee.api.plugin.Plugin;

public class BungeeEventHandler extends EventHandler<Plugin> {

	private final Plugin plugin;

	protected BungeeEventHandler(Plugin plugin) {
		super(plugin.getLogger());
		this.plugin = plugin;
	}

	@Override
	protected void runTask(Boolean sync, Plugin plugin, Runnable run) {
		plugin.getProxy().getScheduler().runAsync(plugin, run);
	}

	public Plugin getPlugin() {
		return plugin;
	}



}
