package com.mickare.xserver.commands.XServerSubCommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.BukkitXServerManager;
import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.exceptions.NotInitializedException;

public class Reconnect extends SubCommand {

	public Reconnect(XServerPlugin plugin) {
		super(plugin, "reconnect", "[s]", "Reconnects all servers. It accepts one flag (s) for softly reconnect only those not connected servers.");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		boolean soft = false;
		if(args.length > 0) {
			if(args[0].equalsIgnoreCase("s")) {
				soft = true;
			} else {
				sender.sendMessage(ChatColor.RED + "Error, wrong parameters.");
				sender.sendMessage(ChatColor.GRAY + "Parameters: " + this.getArguments());
				return true;
			}
		}
		try {
			if(soft) {
				BukkitXServerManager.getInstance().reconnectAll_soft();
			} else {
				BukkitXServerManager.getInstance().reconnectAll_forced();
			}
		} catch (NotInitializedException e) {
			sender.sendMessage(ChatColor.RED + "XServer isn't initialized!");
			return false;
		}
		return true;
	}

}
