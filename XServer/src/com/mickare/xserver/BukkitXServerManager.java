package com.mickare.xserver;

import java.util.logging.Logger;

import com.mickare.xserver.exceptions.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import com.mickare.xserver.util.MySQL;

public class BukkitXServerManager extends XServerManager{
	
	private final JavaPlugin plugin;
	
	protected BukkitXServerManager(String servername, JavaPlugin plugin, Logger logger, MySQL connection) throws InvalidConfigurationException {
		super(servername, logger, connection, new EventHandler(plugin));
		this.plugin = plugin;
	}

	@Override
	public void stopServer() {
		plugin.getServer().shutdown();
	}
	
}
