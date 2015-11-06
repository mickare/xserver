package de.mickare.xserver.commands.XServerSubCommands;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import de.mickare.xserver.XServerPlugin;
import de.mickare.xserver.commands.SubCommand;
import de.mickare.xserver.net.XServer;

public class Status extends SubCommand {
	
	public Status( XServerPlugin plugin ) {
		super( plugin, "status", "", "Shows the connection status of the servers." );
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void execute( CommandSender sender, String[] args ) {
		StringBuilder sb = new StringBuilder();
		
		LinkedList<XServer> servers = new LinkedList<XServer>( getPlugin().getManager().getServers() );
		
		Collections.sort( servers, new Comparator<XServer>() {
			@Override
			public int compare( XServer o1, XServer o2 ) {
				return o1.getName().compareTo( o2.getName() );
			}
		} );
		
		for ( XServer s : servers ) {
			sb.append( "\n" ).append( ChatColor.RESET ).append( s.getName() ).append( ChatColor.GRAY ).append( " : " );
			if ( s.isConnected() ) {
				sb.append( ChatColor.GREEN ).append( "connected" );
				sb.append( ChatColor.GRAY ).append( "(" ).append( s.countConnections() ).append( ")" );
			} else {
				sb.append( ChatColor.RED ).append( "not connected" );
			}
		}
		
		sender.sendMessage( sb.toString() );
		return;
	}
	
}