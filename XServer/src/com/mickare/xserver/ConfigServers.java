package com.mickare.xserver;

import java.io.File;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;


import com.mickare.xserver.Exception.NotInitializedException;
import com.mickare.xserver.config.ConfigAccessor;

public class ConfigServers {

	// Static
	
	private static ConfigServers instance = null;
	private static final String CONFIGFILE = "servers.yml";
	
	/**
	 * Get the instance of the configuration server manager
	 * @return instance
	 * @throws NotInitializedException
	 */
	public static ConfigServers getInstance() throws NotInitializedException {
		if(instance == null) {
			throw new NotInitializedException("ConfigServers not initialized!");
		}
		return instance;
	}
	
	protected static void initialize(JavaPlugin plugin) throws InvalidConfigurationException {
		instance = new ConfigServers(plugin);		
	}
	
	// Normal
	


	private final ConfigAccessor ca;
	private final XServer homeServer;

	private final HashSet<XServer> servers = new HashSet<XServer>();

	private ConfigServers(JavaPlugin plugin)
			throws InvalidConfigurationException {
		
		boolean useGlobalConfig = plugin.getConfig().getBoolean("server.useGlobalServerConfig");
		String globalConfigFileName = plugin.getConfig().getString("server.globalServerConfig");
		File globalConfigFile = new File(globalConfigFileName);
		
		
		if(useGlobalConfig && globalConfigFile.exists()) {
			plugin.getLogger().info("Using global server configurations in: \"" + globalConfigFileName + "\"");
			this.ca = new ConfigAccessor(plugin, globalConfigFile);
		} else {	
			plugin.getLogger().info("Using local server configurations in plugin folder: \"" + CONFIGFILE + "\"");
			this.ca = new ConfigAccessor(plugin, CONFIGFILE);
		}
		ca.saveDefaultConfig();
		reload();
		
		
		// HOMESERVER
		String servername = plugin.getServer().getMotd();
		if (!plugin.getConfig().getBoolean("server.useMotd")) {
			servername = plugin.getConfig().getString("servername");
		}

		if (servername == null) {
			servername = plugin.getServer().getServerName();
		}

		homeServer = getServer(servername);

		if (homeServer == null) {
			throw new InvalidConfigurationException(
					"Server information for \"" + servername
							+ "\" was not found in \"server.yml\"");
		}
		
	}
	
	
	/**
	 * Reload configuration
	 * @throws InvalidConfigurationException
	 */
	public synchronized void reload() throws InvalidConfigurationException {
		ca.reloadConfig();
		servers.clear();
		ConfigurationSection cs = ca.getConfig().getConfigurationSection(
				"servers");
		Set<String> servernames = cs.getKeys(false);
		for (String servername : servernames) {
			ConfigurationSection serverSection = cs
					.getConfigurationSection(servername);
			String[] hostip = serverSection.getString("address").split(":");
			if (hostip.length != 2) {
				throw new InvalidConfigurationException(
						"Invalid configuration: " + CONFIGFILE + " for "
								+ servername);
			}
			servers.add(new XServer(servername, hostip[0], Integer
					.valueOf(hostip[1]), serverSection.getString("pw")));
		}
	}

	/**
	 * Get the list of all available servers
	 * @return servers
	 */
	public synchronized Set<XServer> getServers() {
		return new HashSet<XServer>(servers);
	}

	/**
	 * Get a string list of all servernames
	 * @return
	 */
	public synchronized String[] getServernames() {
		return servers.toArray(new String[servers.size()]);
	}

	/**
	 * Get the XServer Object with the servername name
	 * @param name servername
	 * @return XServer with that name
	 */
	public synchronized XServer getServer(String name) {
		for (XServer s : servers) {
			if (s.getName().equalsIgnoreCase(name)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Get the XServer Object via host and port
	 * @param host
	 * @param port
	 * @return XServer
	 */
	public synchronized XServer getServer(String host, int port) {
		for (XServer s : servers) {
			if (s.getHost().equalsIgnoreCase(host) && s.getPort() == port) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Get the XServer Object via remote Socket Address
	 * @param remoteSocketAddress
	 * @return XServer
	 */
	public XServer getServer(SocketAddress remoteSocketAddress) {
		String tmp = remoteSocketAddress.toString();
		String host = remoteSocketAddress.toString();
		int port = 0;
		if (tmp.startsWith("/")) {
			tmp = tmp.substring(1);
		}
		if (tmp.contains(":")) {
			String[] stmp = tmp.split(":");
			host = stmp[0];
			port = Integer.valueOf(stmp[1]);
		}
		/*
		 * XServerPlugin .getInstance() .getLogger() .info("*****\nHost: '" +
		 * host + "'\nPort: " + port + "\n******");
		 */
		return getServer(host, port);
	}

	/**
	 * Is the adress host:port a server from config?
	 * @param address
	 * @return true if it is
	 */
	public boolean isServer(String address) {
		String[] tmp = address.split(":");
		if (tmp.length == 2) {
			return isServer(tmp[0], Integer.valueOf(tmp[1]));
		}
		return false;
	}

	/**
	 * Is this a server from config?
	 * @param servername
	 * @return true if true ^^
	 */
	public synchronized boolean isServerByName(String servername) {
		return getServer(servername) != null;
	}

	/**
	 * Is this host:port a server from config?
	 * @param host
	 * @param port
	 * @return true if true
	 */
	public boolean isServer(String host, int port) {
		return getServer(host, port) != null;
	}

	public synchronized boolean checkPassword(String servername, String password) {
		for (XServer xs : servers) {
			if (xs.getName().equals(servername)
					&& xs.getPassword().equals(password)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Has some xserver from config the same host?
	 * @param host
	 * @return true if true
	 */
	public boolean isHost(String host) {
		for (XServer xs : servers) {
			if (xs.getHost().equals(host)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Has some xserver from config the same host, like the remote socket address?
	 * @param remoteSocketAddress
	 * @return true if true
	 */
	public boolean isHost(SocketAddress remoteSocketAddress) {
		String tmp = remoteSocketAddress.toString();
		String host = remoteSocketAddress.toString();
		// int port = 0;
		if (tmp.startsWith("/")) {
			tmp = tmp.substring(1);
		}
		if (tmp.contains(":")) {
			String[] stmp = tmp.split(":");
			host = stmp[0];
			// port = Integer.valueOf(stmp[1]);
		}
		return isHost(host);
	}
	
	/**
	 * This local Server adress... who am i?
	 * @return me
	 */
	public XServer getHomeServer() {
		return homeServer;
	}
}
