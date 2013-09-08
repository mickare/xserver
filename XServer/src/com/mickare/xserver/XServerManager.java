package com.mickare.xserver;

import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;

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
	
	protected XServerManager(String servername,
			XServerPlugin plugin, MySQL connection,
			EventHandler<JavaPlugin> eventhandler)
			throws InvalidConfigurationException, IOException {
		super(servername, plugin, connection, eventhandler);
		// TODO Auto-generated constructor stub
	}


}
