package com.mickare.xserver;

import java.io.IOException;
import java.util.logging.Logger;

import net.md_5.bungee.api.plugin.Plugin;

import com.mickare.xserver.commands.XServerCommands;
import com.mickare.xserver.util.MySQL;
import com.mickare.xserver.config.ConfigAccessor;
import com.mickare.xserver.exceptions.InvalidConfigurationException;

public class XServerPlugin extends Plugin {

	private Logger log = null;

	private String servername;

	private MySQL cfgconnection = null;
	private XServerManager xmanager;
	private ConfigAccessor config;

	@Override
	public void onDisable() {
		log = Logger.getLogger("BungeeCord");
		log.info("---------------------------------");
		log.info("--------- Proxy XServer ---------");
		log.info("----------  disabling  ----------");

		try {
			if (xmanager != null) {
				xmanager.stop();
			}
		} catch (IOException e) {
			log.severe("[ERROR] A Error occured when disabling plugin!\n[ERROR] "
					+ e.getMessage());
		}

		if (cfgconnection != null) {
			cfgconnection.disconnect();
		}

		log.info(getDescription().getName() + " disabled!");
	}

	@Override
	public void onEnable() {
		log = Logger.getLogger("BungeeCord");
		log.info("---------------------------------");
		log.info("--------- Proxy XServer ---------");
		log.info("----------  enabling   ----------");

		servername = this.getConfig().getString("servername");

		String user = this.getConfig().getString("mysql.User");
		String pass = this.getConfig().getString("mysql.Pass");
		String data = this.getConfig().getString("mysql.Data");
		String host = this.getConfig().getString("mysql.Host");

		log.info("Connecting to Database " + host + "/" + data + " with user: " + user);
		
		cfgconnection = new MySQL(log, user, pass, data, host, "config");
		cfgconnection.connect();

		try {
			xmanager = new XServerManager(servername, log,
					cfgconnection);
			
			log.info("Starting XServer async.");
			xmanager.start_async();
		} catch (InvalidConfigurationException e) {
			log.severe("XServerManager not initialized correctly!");
			log.severe(e.getMessage());
			// this.getServer().dispatchCommand(this.getServer().getConsoleSender(),
			// "stop");
		}

		

		// Register Commands
		new XServerCommands(this);

		log.info(getDescription().getName() + " enabled!");
	}

	public ConfigAccessor getConfig() {
		if (config == null) {
			config = new ConfigAccessor(this, "config.yml");
			config.saveDefaultConfig();
		}
		return config;
	}

	public Logger getLogger() {
		return log;
	}

}
