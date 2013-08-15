package com.mickare.xserver.commands.XServerSubCommands;


import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import com.mickare.xserver.XServerManager;
import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.exceptions.NotInitializedException;

public class Reconnect extends SubCommand {

	public Reconnect(XServerPlugin plugin) {
		super(plugin, "reconnect", "[s]", "Reconnects all servers. It accepts one flag (s) for softly reconnect only those not connected servers.");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		boolean soft = false;
		if(args.length > 0) {
			if(args[0].equalsIgnoreCase("s")) {
				soft = true;
			} else {
				sender.sendMessage(ChatColor.RED + "Error, wrong parameters.");
				sender.sendMessage(ChatColor.GRAY + "Parameters: " + this.getArguments());
				return;
			}
		}
		try {
			if(soft) {
				XServerManager.getInstance().reconnectAll_soft();
			} else {
				XServerManager.getInstance().reconnectAll_forced();
			}
		} catch (NotInitializedException e) {
			sender.sendMessage(ChatColor.RED + "XServer isn't initialized!");
			return;
		}
		return;
	}

}
