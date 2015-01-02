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
		log.info( "---------------------------------" );
		log.info( "------------ XServer ------------" );
		log.info( "----------  disabling  ----------" );
		
		if ( xmanager != null ) {
			xmanager.stop();
		}
		
		if ( cfgconnection != null ) {
			cfgconnection.disconnect();
		}
		
		log.info( getDescription().getName() + " disabled!" );
	}
	
	@Override
	public void onEnable() {
		log = this.getLogger();
		log.info( "---------------------------------" );
		log.info( "------------ XServer ------------" );
		log.info( "----------  enabling   ----------" );
		
		this.saveDefaultConfig();
		
		servername = this.getServer().getMotd();
		if ( servername == null ) {
			servername = this.getServer().getServerName();
		}
		if ( !this.getConfig().getBoolean( "useMotdForServername", true ) ) {
			servername = this.getConfig().getString( "servername", servername );
		}
		
		String user = this.getConfig().getString( "mysql.User" );
		String pass = this.getConfig().getString( "mysql.Pass" );
		String data = this.getConfig().getString( "mysql.Data" );
		String host = this.getConfig().getString( "mysql.Host" );
		int port = this.getConfig().getInt( "mysql.Port", 3306 );
		String sql_table_xservers = this.getConfig().getString( "mysql.TableXServers", "xservers" );
		String sql_table_xgroups = this.getConfig().getString( "mysql.TableXGroups", "xgroups" );
		String sql_table_xserversxgroups = this.getConfig().getString( "mysql.TableXServersGroups", "xservers_xgroups" );
		
		log.info( "Connecting to Database " + host + ":" + port + "/" + data + " with user: " + user );
		
		cfgconnection = new MySQL( log, user, pass, data, host, port, "config" );
		
		cfgconnection.connect();
		
		try {
			log.info( "Starting XServer async." );
			xmanager = new BukkitXServerManager( servername, this, cfgconnection, sql_table_xservers,
					sql_table_xgroups, sql_table_xserversxgroups );
		} catch ( IOException | InvalidConfigurationException e ) {
			log.severe( "XServerManager not initialized correctly!\n" + e.getMessage() + "\n"
					+ MyStringUtils.stackTraceToString( e ) );
			this.getServer().shutdown();
			// this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "stop");
		}
		
		// Register Commands
		new XServerCommands( this );
		
		log.info( getDescription().getName() + " enabled!" );
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
