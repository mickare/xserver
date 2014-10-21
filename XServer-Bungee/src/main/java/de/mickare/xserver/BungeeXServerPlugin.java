package de.mickare.xserver;

import java.io.IOException;
import java.util.logging.Logger;

import net.md_5.bungee.api.plugin.Plugin;
import de.mickare.xserver.commands.XServerCommands;
import de.mickare.xserver.config.ConfigAccessor;
import de.mickare.xserver.exceptions.InvalidConfigurationException;
import de.mickare.xserver.util.MySQL;
import de.mickare.xserver.util.MyStringUtils;

public class BungeeXServerPlugin extends Plugin implements XServerPlugin{

	private static final long AUTORECONNECT = 10000;
	public static final XType HOMETYPE = XType.BungeeCord;
	
        private Logger log = null;

        private String servername;

        private MySQL cfgconnection = null;
        private BungeeXServerManager xmanager;
        private ConfigAccessor config;

        @Override
        public void onDisable() {
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
                log = this.getLogger();
                log.info("---------------------------------");
                log.info("--------- Proxy XServer ---------");
                log.info("----------  enabling   ----------");

                servername = this.getConfig().getString("servername");

                String user = this.getConfig().getString("mysql.User");
                String pass = this.getConfig().getString("mysql.Pass");
                String data = this.getConfig().getString("mysql.Data");
                String host = this.getConfig().getString("mysql.Host");
                int port = this.getConfig().getInt("mysql.Port", 3306);
                String sql_table_xservers = this.getConfig().getString("mysql.TableXServers", "xservers");
    			String sql_table_xgroups = this.getConfig().getString("mysql.TableXGroups", "xgroups");
    			String sql_table_xserversxgroups = this.getConfig().getString("mysql.TableXServersGroups", "xservers_xgroups");

                log.info("Connecting to Database " + host + "/" + data + " with user: " + user);
                
                cfgconnection = new MySQL(log, user, pass, data, host, port, "config");
                cfgconnection.connect();

                try {
                        log.info("Starting XServer async.");
                        xmanager = new BungeeXServerManager(servername, this,
                                        cfgconnection, sql_table_xservers, sql_table_xgroups, sql_table_xserversxgroups);
                        
                        
                } catch (InvalidConfigurationException | IOException e) {
                        log.severe("XServerManager not initialized correctly!\n" + e.getMessage() + "\n" + MyStringUtils.stackTraceToString(e));
                        // this.getServer().dispatchCommand(this.getServer().getConsoleSender(),
                        // "stop");
                }

                

                // Register Commands
                this.getProxy().getPluginManager().registerCommand(this, new XServerCommands(this));

                log.info(getDescription().getName() + " enabled!");
        }

        public ConfigAccessor getConfig() {
                if (config == null) {
                        config = new ConfigAccessor(this, "config.yml");
                        config.saveDefaultConfig();
                }
                return config;
        }

		@Override
		public void shutdownServer() {
			this.getProxy().stop();
		}

		@Override
		public BungeeXServerManager getManager() {
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