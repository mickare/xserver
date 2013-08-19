package com.mickare.xserver.commands.XServerSubCommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.BukkitXServerManager;
import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.net.BukkitPing;
import com.mickare.xserver.net.XServer;

public class PingCommand extends SubCommand {

	public PingCommand(XServerPlugin plugin) {
		super(plugin, "ping", "[Servername]", "Ping all servers, or the one as given.");
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		try {
			if(args.length > 0) {
				XServer s = BukkitXServerManager.getInstance().getServer(args[0]);
				if(s == null) {
					sender.sendMessage(ChatColor.RED + "Server \"" + args[0] + "\" not found!");
					return true;
				}
				BukkitPing p = new BukkitPing(sender, BukkitXServerManager.getInstance().homeServer.getName());
				p.add(s);
				p.start();
			} else {
				
				BukkitPing p = new BukkitPing(sender, BukkitXServerManager.getInstance().homeServer.getName());
				p.addAll(BukkitXServerManager.getInstance().getServers());
				p.start();
				
			}
		} catch (NotInitializedException e) {
			sender.sendMessage(ChatColor.RED + "XServer isn't initialized!");
			return true;
		}
		return true;
	}

}
