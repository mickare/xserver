package de.mickare.xserver;

import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import de.mickare.xserver.commands.XServerCommands;
import de.mickare.xserver.exceptions.InvalidConfigurationException;
import de.mickare.xserver.util.MySQL;
import de.mickare.xserver.util.MyStringUtils;

public class BukkitXServerPlugin extends JavaPlugin implements XServerPlugin {
	
	private static final long AUTORECONNECT = 10000;
	public static final XType HOMETYPE = XType.Bukkit;
	
	private Logger log;
	
	private String servername;
	
	private MySQL cfgconnection = null;
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
			log.severe("[ERROR] A Error occured while disabling xserver plugin!\n[ERROR] " + e.getMessage());
		}
		
		if(cfgconnection != null) {
			cfgconnection.disconnect();
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
		
		String user = this.getConfig().getString("mysql.User");
		String pass = this.getConfig().getString("mysql.Pass");
		String data = this.getConfig().getString("mysql.Data");
		String host = this.getConfig().getString("mysql.Host");

		log.info("Connecting to Database " + host + "/" + data + " with user: " + user);
		
		cfgconnection = new MySQL(log, user, pass, data, host, "config");
		
		cfgconnection.connect();
		
		try {
			log.info("Starting XServer async.");
			xmanager = new XServerManager(servername, this, cfgconnection);
		} catch (IOException | InvalidConfigurationException e) {
			log.severe("XServerManager not initialized correctly!\n" + e.getMessage() + "\n" + MyStringUtils.stackTraceToString(e));
			this.getServer().shutdown();
			//this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "stop");
		}
		

		//Register Commands
		new XServerCommands(this);
		
		log.info(getDescription().getName() + " enabled!");
	}

	@Override
	public void shutdownServer() {
		this.getServer().shutdown();
	}

	@Override
	public XServerManager getManager() {
		return xmanager;
	}

	@Override
	public long getAutoReconnectTime() {
		return AUTORECONNECT;
	}

	@Override
	public XType getHomeType() {
		return HOMETYPE;
	}
	
}
