package com.mickare.xserver;

import java.io.IOException;

import com.mickare.xserver.exceptions.InvalidConfigurationException;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.MySQL;

public class XServerManager extends AbstractXServerManager {

	// In Milliseconds

	private static XServerManager instance = null;

	public static XServerManager getInstance() throws NotInitializedException {
		if (instance == null) {
			throw new NotInitializedException();
		}
		return instance;
	}

	private final BukkitEventHandler eventhandler;

	protected XServerManager(String servername, BukkitXServerPlugin plugin,
			MySQL connection) throws InvalidConfigurationException, IOException {
		super(servername, plugin, connection);
		instance = this;
		this.eventhandler = new BukkitEventHandler(plugin);
	}

	@Override
	public BukkitEventHandler getEventHandler() {
		return eventhandler;
	}

}
