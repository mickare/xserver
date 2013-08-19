package com.mickare.xserver.commands.XServerSubCommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.BukkitXServerManager;
import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.exceptions.NotInitializedException;

public class Reload extends SubCommand {

	public Reload(XServerPlugin plugin) {
		super(plugin, "reload", "", "Reloads the server list and reconnects.");
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		try {
			BukkitXServerManager.getInstance().reload();
			sender.sendMessage("Reload done and now connecting to servers...");
			BukkitXServerManager.getInstance().getThreadPool().runTask(new Runnable() {
				public void run() {
					try {
						BukkitXServerManager.getInstance().reconnectAll_forced();
					} catch (NotInitializedException e) {
						getPlugin().getLogger().severe(e.getMessage());
					}
				}});
		} catch (NotInitializedException e) {
			sender.sendMessage(ChatColor.RED + "XServer isn't initialized!");
		}
		return true;
	}

}
