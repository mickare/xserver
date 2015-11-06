package de.mickare.xserver;

import java.io.IOException;

import de.mickare.xserver.exceptions.InvalidConfigurationException;
import de.mickare.xserver.exceptions.NotInitializedException;
import de.mickare.xserver.listener.StressTestListener;
import de.mickare.xserver.util.MySQL;

public class BukkitXServerManager extends AbstractXServerManager {
	
	public static BukkitXServerManager getInstance() throws NotInitializedException {
		return ( BukkitXServerManager ) XServerManager.getInstance();
	}
	
	private final BukkitEventHandler eventhandler;
	private final BukkitXServerPlugin bukkitPlugin;
	private final StressTestListener stressListener;
	
	protected BukkitXServerManager( String servername, BukkitXServerPlugin bukkitPlugin, MySQL connection,
			String sql_table_xservers, String sql_table_xgroups, String sql_table_xserversxgroups )
			throws InvalidConfigurationException, IOException {
		super( servername, bukkitPlugin, connection, sql_table_xservers, sql_table_xgroups, sql_table_xserversxgroups,
				new BukkitServerThreadPool( bukkitPlugin ) );
		this.eventhandler = new BukkitEventHandler( bukkitPlugin );
		this.bukkitPlugin = bukkitPlugin;
		this.stressListener = new StressTestListener( this );
		registerOwnListeners();
	}
	
	@Override
	public BukkitEventHandler getEventHandler() {
		return eventhandler;
	}
	
	@Override
	public void registerOwnListeners() {
		this.getEventHandler().registerListener( bukkitPlugin, stressListener );
	}
	
	@Override
	public void stop() throws IOException {
		try {
			this.getThreadPool().shutDown();
		} finally {
			super.stop();
		}
	}
	
}
