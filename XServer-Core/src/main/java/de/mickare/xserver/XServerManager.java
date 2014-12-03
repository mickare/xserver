package de.mickare.xserver;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

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
	
	protected XServerManager(String servername, XServerPlugin plugin, MySQL connection, String sql_table_xservers,
			String sql_table_xgroups, String sql_table_xserversxgroups, ServerThreadPoolExecutor stp)
			throws InvalidConfigurationException, IOException {
		super( servername, plugin, connection, sql_table_xservers, sql_table_xgroups, sql_table_xserversxgroups, stp );
		instance = this;
		
		
	}


}
