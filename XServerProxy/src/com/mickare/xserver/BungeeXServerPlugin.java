package com.mickare.xserver;

import java.io.IOException;
import java.util.logging.Logger;

import net.md_5.bungee.api.plugin.Plugin;

import com.mickare.xserver.commands.XServerCommands;
import com.mickare.xserver.util.MySQL;
import com.mickare.xserver.util.MyStringUtils;
import com.mickare.xserver.config.ConfigAccessor;
import com.mickare.xserver.exceptions.InvalidConfigurationException;

public class BungeeXServerPlugin extends Plugin implements XServerPlugin{

	private static final long AUTORECONNECT = 10000;
	public static final XType HOMETYPE = XType.BungeeCord;
	
        private Logger log = null;

        private String servername;

        private MySQL cfgconnection = null;
        private XServerManagerBungee xmanager;
        private ConfigAccessor config;

        @Override
        public void onDisable() {
                log = Logger.getLogger("BungeeCord");
                log = this.getLogger();
                log.info("---------------------------------");
                log.info("--------- Proxy XServer ---------");
                log.info("----------  disabling  ----------");

                try {
                        if (xmanager != null) {
                                xmanager.stop();
                        }
                } catch (IOException e) {
                        log.severe("[ERROR] A Error occured while disabling xserver plugin!\n[ERROR] "
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
                log = this.getLogger();
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
                        log.info("Starting XServer async.");
                        xmanager = new XServerManagerBungee(servername, this,
                                        cfgconnection);
                        
                        
                } catch (InvalidConfigurationException | IOException e) {
                        log.severe("XServerManager not initialized correctly!\n" + e.getMessage() + "\n" + MyStringUtils.stackTraceToString(e));
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

		@Override
		public void shutdownServer() {
			this.getProxy().stop();
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