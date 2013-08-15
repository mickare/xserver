package com.mickare.xserver.commands.XServerSubCommands;


import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import com.mickare.xserver.XServerManager;
import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.net.Ping;
import com.mickare.xserver.net.XServer;

public class PingCommand extends SubCommand {

	public PingCommand(XServerPlugin plugin) {
		super(plugin, "ping", "[Servername]", "Ping all servers, or the one as given.");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		try {
			if(args.length > 0) {
				XServer s = XServerManager.getInstance().getServer(args[0]);
				if(s == null) {
					sender.sendMessage(ChatColor.RED + "Server \"" + args[0] + "\" not found!");
					return;
				}
				Ping p = new Ping(sender, XServerManager.getInstance().homeServer.getName());
				p.add(s);
				p.start();
			} else {
				
				Ping p = new Ping(sender, XServerManager.getInstance().homeServer.getName());
				p.addAll(XServerManager.getInstance().getServers());
				p.start();
				
			}
		} catch (NotInitializedException e) {
			sender.sendMessage(ChatColor.RED + "XServer isn't initialized!");
			return;
		}
		return;
	}

}
