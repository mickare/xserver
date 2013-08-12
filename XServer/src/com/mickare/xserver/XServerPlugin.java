package com.mickare.xserver;

import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import com.mickare.xserver.commands.XServerCommands;
import com.mickare.xserver.util.MySQL;

public class XServerPlugin extends JavaPlugin {
	
	private Logger log;
	
	private String servername;
	
	private MySQL statsconnection = null;
	private XServerManager xmanager;
	
	@Override
	public void onDisable() {
		log = this.getLogger();
		log.info("---------------------------------");
		log.info("------------ XServer ------------");
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
		log.info("------------ XServer ------------");
		log.info("----------  enabling   ----------");
		
		servername = this.getServer().getMotd();
		if (servername == null) {
			servername = this.getServer().getServerName();
		}
		
		this.saveDefaultConfig();
		
		statsconnection = new MySQL(log , this.getConfig().getString("mysql.User", ""), this.getConfig().getString("mysql.Pass", ""), this.getConfig().getString("mysql.Data", ""), this.getConfig().getString("mysql.Host", ""), "stats");
		statsconnection.connect();
		
		try {
			xmanager = new XServerManager(servername, this, log, statsconnection);
		} catch (InvalidConfigurationException e) {
			log.severe("XServerManager not initialized correctly!");
			log.severe(e.getMessage());
			this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "stop");
		}
		
		log.info("Starting XServer async.");
		xmanager.start_async();

		//Register Commands
		new XServerCommands(this);
		
		log.info(getDescription().getName() + " enabled!");
	}
	
}
