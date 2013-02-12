package com.mickare.xserver;

import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import com.mickare.xserver.Exception.NotInitializedException;

public class XServerPlugin extends JavaPlugin {
	
	private Logger log;
	
	@Override
	public void onDisable() {
		log = this.getLogger();
		log.info("---------------------------------");
		log.info("------------ XServer ------------");
		log.info("----------  disabling  ----------");
		
		try {
			ServerMain.getInstance().stop();
		} catch (IOException | NotInitializedException e) {
			this.onDisable();
		}
		
		log.info(getDescription().getName() + " disabled!");
	}
	
	@Override
	public void onEnable() {		
		log = this.getLogger();
		log.info("---------------------------------");
		log.info("------------ XServer ------------");
		log.info("----------  enabling   ----------");
		
		try {
			ConfigServers.initialize(this); 
		} catch (InvalidConfigurationException e) {
			this.onDisable();
			return;
		}
		
		EventHandler.initialize(this);
		try {
			MessageFactory.initialize(ConfigServers.getInstance());
		} catch (NotInitializedException e1) {
			this.onDisable();
		}
		
		try {
			ServerMain.initialize(ConfigServers.getInstance().getHomeServer());
		} catch (NotInitializedException e) {
		}
		
		try {
			ServerMain.getInstance().start();
		} catch (IOException | NotInitializedException e) {
			this.onDisable();
		}
		
		log.info(getDescription().getName() + " enabled!");
	}
	
	
}
