package de.mickare.xserver;

import java.io.IOException;

import de.mickare.xserver.exceptions.InvalidConfigurationException;
import de.mickare.xserver.exceptions.NotInitializedException;
import de.mickare.xserver.util.MySQL;

public abstract class XServerManager extends AbstractXServerManagerObj {

	private static XServerManager instance = null;

	public static XServerManager getInstance() throws NotInitializedException {
		if (instance == null) {
			throw new NotInitializedException();
		}
		return instance;
	}
	
	protected XServerManager(String servername, XServerPlugin plugin, MySQL connection, String sql_table, ServerThreadPoolExecutor stpool)
			throws InvalidConfigurationException, IOException {
		super( servername, plugin, connection, sql_table, stpool );
		instance = this;
	}


}
