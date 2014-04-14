package de.mickare.xserver;

import java.io.IOException;

import de.mickare.xserver.exceptions.InvalidConfigurationException;
import de.mickare.xserver.exceptions.NotInitializedException;
import de.mickare.xserver.listener.StressTestListener;
import de.mickare.xserver.util.MySQL;

public class XServerManager extends AbstractXServerManagerObj {

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
			MySQL connection, String sql_table) throws InvalidConfigurationException, IOException {
		super(servername, bungeePlugin, connection, sql_table);
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