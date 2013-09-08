package com.mickare.xserver.commands.XServerSubCommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.BukkitXServerPlugin;
import com.mickare.xserver.commands.SubCommand;

public class Reconnect extends SubCommand {

	public Reconnect(BukkitXServerPlugin plugin) {
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

			if(soft) {
				getPlugin().getManager().reconnectAll_soft();
			} else {
				getPlugin().getManager().reconnectAll_forced();
			}

		return true;
	}

}
