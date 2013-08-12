package com.mickare.xserver;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.net.SocketFactory;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.net.XServer;
import com.mickare.xserver.util.MySQL;

public class XServerManager {

	private static XServerManager instance = null;
	
	public static XServerManager getInstance() throws NotInitializedException {
		if(instance == null) {
			throw new NotInitializedException();
		}
		return instance;
	}
	
	private Logger logger;
	private EventHandler eventhandler;
	private ServerThreadPoolExecutor stpool;
	private SocketFactory sf;
	private MainServer mainserver;
	
	private final MySQL connection;
	public final XServer homeServer;
	private final HashMap<String, XServer> servers = new HashMap<String, XServer>();
	
	protected XServerManager(String servername, JavaPlugin plugin, Logger logger, MySQL connection) throws InvalidConfigurationException {
		instance = this;
		this.logger = logger;
		stpool = new ServerThreadPoolExecutor();
		sf = SocketFactory.getDefault();
		this.connection = connection;
		this.reload();
		homeServer = getServer(servername);
		if (homeServer == null) {
			throw new InvalidConfigurationException("Server information for \"" + servername + "\" was not found!");
		}
		
		this.eventhandler = new EventHandler(plugin);		
		mainserver = new MainServer(this);
	}
	
	public void start() throws IOException {
		mainserver.start();
		reconnectAll_soft();
	}
	
	public void start_async() {
		stpool.runTask(new Runnable() {
			public void run() {
				try {
					start();
				} catch (Exception e) {
					logger.severe("XServer not started correctly!");
					logger.severe(e.getMessage());
					Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "stop");;
				}
			}});
	}
	
	public void reconnectAll_soft() {
		for (XServer s : servers.values()) {
			if(!s.isConnected()) {
				try {
					s.connect();
				} catch (IOException | InterruptedException | NotInitializedException e) {
					logger.info("Connection to " +  s.getName() + " failed!\n" + e.getMessage());
				}
			}
		}
	}
	
	public void reconnectAll_forced() {
		for (XServer s : servers.values()) {
			try {
				s.connect();
			} catch (IOException | InterruptedException | NotInitializedException e) {
				logger.info("Connection to " +  s.getName() + " failed!\n" + e.getMessage());
			}
		}
	}
	
	public void stop() throws IOException {
		mainserver.stop();
		stpool.shutDown();
	}
	
	/**
	 * Reload configuration
	 * 
	 * @throws InvalidConfigurationException
	 */
	public void reload() {
		synchronized(servers) {
			for(XServer s : servers.values()) {
				s.disconnect();
			}
			servers.clear();
	
			ResultSet rs = null;
			synchronized(connection) {
				rs = connection.query("SELECT * FROM xserver");
			}
			
			if(rs != null) {
				try {
					while (rs.next()) {
						String servername = rs.getString("NAME");
						String[] hostip = rs.getString("ADRESS").split(":");
						String pw = rs.getString("PW");
						servers.put(servername, new XServer(servername, hostip[0], Integer.valueOf(hostip[1]), pw));
						//System.out.println(servername + " " + hostip[0] + " " + Integer.valueOf(hostip[1]) + " " +  pw);
					}
				} catch (NumberFormatException | SQLException e) {
					logger.severe(e.getMessage());
				}
			} else {
				logger.severe("Couldn't load XServer List form Database!");
			}
		}
	}
			
	public XServer getHomeServer() {
		return homeServer;
	}

	public XServer getServer(String servername) {
		synchronized(servers) {
		return servers.get(servername);
		}
	}

	public Logger getLogger() {
		return logger;
	}

	public ServerThreadPoolExecutor getThreadPool() {
		return stpool;
	}

	public SocketFactory getSocketFactory() {
		return sf;
	}
	
	public XServer getXServer(String name) {
		return servers.get(name);
	}
	
	/**
	 * Get the list of all available servers
	 * 
	 * @return servers
	 */
	public synchronized Set<XServer> getServers() {
		synchronized(servers) {
		return new HashSet<XServer>(servers.values());
		}
	}

	/**
	 * Get a string list of all servernames
	 * 
	 * @return
	 */
	public synchronized String[] getServernames() {
		synchronized(servers) {
		return servers.values().toArray(new String[servers.size()]);
		}
	}

	/**
	 * Get the XServer Object with the servername name
	 * 
	 * @param name
	 *            servername
	 * @return XServer with that name
	 */
	public synchronized XServer getServerIgnoreCase(String name) {
		synchronized(servers) {
		for (XServer s : servers.values()) {
			if (s.getName().equalsIgnoreCase(name)) {
				return s;
			}
		}
		return null;
		}
	}
	
	/**
	 * Get the XServer Object via host and port
	 * 
	 * @param host
	 * @param port
	 * @return XServer
	 */
	public synchronized XServer getServer(String host, int port) {
		synchronized(servers) {
		for (XServer s : servers.values()) {
			if (s.getHost().equalsIgnoreCase(host) && s.getPort() == port) {
				return s;
			}
		}
		return null;
		}
	}

	public EventHandler getEventHandler() {
		return eventhandler;
	}
	
}
