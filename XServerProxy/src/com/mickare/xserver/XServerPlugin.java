package com.mickare.xserver;

import java.io.IOException;
import java.util.logging.Logger;

import net.md_5.bungee.api.plugin.Plugin;

import com.mickare.xserver.commands.XServerCommands;
import com.mickare.xserver.util.MySQL;
import com.mickare.xserver.config.ConfigAccessor;
import com.mickare.xserver.exceptions.InvalidConfigurationException;

public class XServerPlugin extends Plugin {
	
	private Logger log;
	
	private String servername;
	
	private MySQL statsconnection = null;
	private XServerManager xmanager;
	private ConfigAccessor config;
	
	@Override
	public void onDisable() {
		log = this.getLogger();
		log.info("---------------------------------");
		log.info("--------- Proxy XServer ---------");
		log.info("----------  disabling  ----------");
		
		try {
			if(xmanager != null) {
				xmanager.stop();
			}
		} catch (IOException e) {
			log.severe("[ERROR] A Error occured when disabling plugin!\n[ERROR] " + e.getMessage());
		}
		
		if(statsconnection != null) {
			statsconnection.disconnect();
		}
		
		log.info(getDescription().getName() + " disabled!");
	}
	
	@Override
	public void onEnable() {		
		log = this.getLogger();
		log.info("---------------------------------");
		log.info("--------- Proxy XServer ---------");
		log.info("----------  enabling   ----------");
		
		servername = this.getConfig().getString("servername");
			
		getLogger().info(this.getConfig().getString("mysql.User") + " " + this.getConfig().getString("mysql.Pass") + " " + this.getConfig().getString("mysql.Data") + " " + this.getConfig().getString("mysql.Host"));
		
		statsconnection = new MySQL(log , this.getConfig().getString("mysql.User"), this.getConfig().getString("mysql.Pass"), this.getConfig().getString("mysql.Data"), this.getConfig().getString("mysql.Host"), "config");
		statsconnection.connect();
		
		try {
			xmanager = new XServerManager(servername, this, log, statsconnection);
		} catch (InvalidConfigurationException e) {
			log.severe("XServerManager not initialized correctly!");
			log.severe(e.getMessage());
			//this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "stop");
		}
		
		log.info("Starting XServer async.");
		xmanager.start_async();

		//Register Commands
		new XServerCommands(this);
		
		log.info(getDescription().getName() + " enabled!");
	}
	
	public ConfigAccessor getConfig() {
		if(config == null) {
			config = new ConfigAccessor(this, "config.yml");
			config.saveDefaultConfig();
		}
		return config;
	}
	
}
