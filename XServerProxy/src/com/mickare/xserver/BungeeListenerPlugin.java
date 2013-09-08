package com.mickare.xserver;

import net.md_5.bungee.api.plugin.Plugin;

public class BungeeListenerPlugin implements XServerListenerPlugin<Plugin> {

	private final Plugin plugin;
	
	public BungeeListenerPlugin(Plugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public Plugin getPlugin() {
		return plugin;
	}

}
