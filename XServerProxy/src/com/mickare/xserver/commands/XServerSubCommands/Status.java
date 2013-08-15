package com.mickare.xserver.commands.XServerSubCommands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import com.mickare.xserver.XServerManager;
import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.net.XServer;

public class Status extends SubCommand {

	public Status(XServerPlugin plugin) {
		super(plugin, "status", "", "Shows the connection status of the servers.");
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		StringBuilder sb = new StringBuilder();
		
		try {
			for(XServer s : XServerManager.getInstance().getServers()) {
				sb.append("\n").append(ChatColor.RESET).append(s.getName()).append(ChatColor.GRAY).append(" : ");
				if(s.isConnected()) {
					sb.append(ChatColor.GREEN).append("connected");
				} else {
					sb.append(ChatColor.RED).append("not connected");
				}
			}
		
		} catch (NotInitializedException e) {
			sb.append(ChatColor.RED).append(e.getMessage());
		}
		
		sender.sendMessage(sb.toString());
		return;
	}

}
