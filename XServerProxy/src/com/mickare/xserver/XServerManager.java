package com.mickare.xserver;

import java.io.IOException;

import com.mickare.xserver.exceptions.InvalidConfigurationException;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.listener.StressTestListener;
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
	private final BungeeXServerPlugin bungeePlugin;
	private final StressTestListener stressListener;
	
	protected XServerManager(String servername, BungeeXServerPlugin bungeePlugin,
			MySQL connection) throws InvalidConfigurationException, IOException {
		super(servername, bungeePlugin, connection);
		instance = this;
		this.eventhandler = new BungeeEventHandler(bungeePlugin);
		this.bungeePlugin = bungeePlugin;
		this.stressListener = new StressTestListener(this);
		registerOwnListeners();
	}

	@Override
	public BungeeEventHandler getEventHandler() {
		return eventhandler;
	}
	
	@Override
	public void registerOwnListeners() {
		this.getEventHandler().registerListener(bungeePlugin, stressListener);	
	}


}