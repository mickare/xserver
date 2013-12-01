package com.mickare.xserver;

import java.io.IOException;

import com.mickare.xserver.exceptions.InvalidConfigurationException;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.listener.StressTestListener;
import com.mickare.xserver.util.MySQL;

public class XServerManagerBukkit extends AbstractXServerManager {

	// In Milliseconds

	private static XServerManager instance = null;

	public static XServerManager getInstance() throws NotInitializedException {
		if (instance == null) {
			throw new NotInitializedException();
		}
		return instance;
	}

	private final BukkitEventHandler eventhandler;
	private final BukkitXServerPlugin bukkitPlugin;
	private final StressTestListener stressListener;

	protected XServerManagerBukkit(String servername, BukkitXServerPlugin bukkitPlugin,
			MySQL connection) throws InvalidConfigurationException, IOException {
		super(servername, bukkitPlugin, connection);
		instance = this;
		this.eventhandler = new BukkitEventHandler(bukkitPlugin);
		this.bukkitPlugin = bukkitPlugin;
		this.stressListener = new StressTestListener(this);
		registerOwnListeners();
	}

	@Override
	public BukkitEventHandler getEventHandler() {
		return eventhandler;
	}

	@Override
	public void registerOwnListeners() {
		this.getEventHandler().registerListener(bukkitPlugin, stressListener);	
	}

}
