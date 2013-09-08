package com.mickare.xserver.commands.XServerSubCommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.XServerManager;
import com.mickare.xserver.BukkitXServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.exceptions.NotInitializedException;
import com.mickare.xserver.net.Ping;
import com.mickare.xserver.net.XServer;
import com.mickare.xserver.user.BukkitComSender;

public class PingCommand extends SubCommand {

	public PingCommand(BukkitXServerPlugin plugin) {
		super(plugin, "ping", "[Servername]", "Ping all servers, or the one as given.");
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		try {
			if(args.length > 0) {
				XServer s = XServerManager.getInstance().getServer(args[0]);
				if(s == null) {
					sender.sendMessage(ChatColor.RED + "Server \"" + args[0] + "\" not found!");
					return true;
				}
				Ping p = new Ping(new BukkitComSender(sender), XServerManager.getInstance().homeServer.getName());
				p.add(s);
				p.start();
			} else {
				
				Ping p = new Ping(new BukkitComSender(sender), XServerManager.getInstance().homeServer.getName());
				p.addAll(XServerManager.getInstance().getServers());
				p.start();
				
			}
		} catch (NotInitializedException e) {
			sender.sendMessage(ChatColor.RED + "XServer isn't initialized!");
			return true;
		}
		return true;
	}

}
