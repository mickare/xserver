package de.mickare.xserver;

import java.io.IOException;

import de.mickare.xserver.exceptions.InvalidConfigurationException;
import de.mickare.xserver.exceptions.NotInitializedException;
import de.mickare.xserver.listener.StressTestListener;
import de.mickare.xserver.util.MySQL;

public class XServerManager extends AbstractXServerManagerObj {

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

	protected XServerManager(String servername, BukkitXServerPlugin bukkitPlugin,
			MySQL connection, String sql_table) throws InvalidConfigurationException, IOException {
		super(servername, bukkitPlugin, connection, sql_table);
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
