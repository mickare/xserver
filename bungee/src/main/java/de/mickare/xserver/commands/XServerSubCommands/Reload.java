package de.mickare.xserver.commands.XServerSubCommands;

import java.io.IOException;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import de.mickare.xserver.BungeeXServerManager;
import de.mickare.xserver.XServerPlugin;
import de.mickare.xserver.commands.SubCommand;
import de.mickare.xserver.exceptions.NotInitializedException;

public class Reload extends SubCommand {
	
	public Reload( XServerPlugin plugin ) {
		super( plugin, "reload", "", "Reloads the server list and reconnects." );
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void execute( CommandSender sender, String[] args ) {
		try {
			getPlugin().getManager().reload();
			sender.sendMessage( "Reload done and now connecting to servers..." );
			getPlugin().getManager().getThreadPool().runTask( new Runnable() {
				public void run() {
					try {
						BungeeXServerManager.getInstance().reconnectAll_forced();
					} catch ( NotInitializedException e ) {
						getPlugin().getLogger().severe( e.getMessage() );
					}
				}
			} );
		} catch ( IOException e1 ) {
			sender.sendMessage( ChatColor.RED + "XServer ERROR: " + e1.getMessage() );
		}
		return;
	}
	
}