package com.mickare.xserver;

import java.io.IOException;

import com.mickare.xserver.exceptions.InvalidConfigurationException;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.util.MySQL;

public class XServerManager extends AbstractXServerManager {

	private static XServerManager instance = null;

	public static XServerManager getInstance() throws NotInitializedException {
		if (instance == null) {
			throw new NotInitializedException();
		}
		return instance;
	}

	private final BungeeEventHandler eventhandler;

	protected XServerManager(String servername, BungeeXServerPlugin plugin,
			MySQL connection) throws InvalidConfigurationException, IOException {
		super(servername, plugin, connection);
		instance = this;
		this.eventhandler = new BungeeEventHandler(plugin);
	}

	@Override
	public BungeeEventHandler getEventHandler() {
		return eventhandler;
	}

}