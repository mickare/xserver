package com.mickare.xserver.commands.XServerSubCommands;

import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.XServerManager;
import com.mickare.xserver.BukkitXServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.exceptions.NotInitializedException;

public class Reload extends SubCommand {

	public Reload(BukkitXServerPlugin plugin) {
		super(plugin, "reload", "", "Reloads the server list and reconnects.");
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		try {
			XServerManager.getInstance().reload();
			sender.sendMessage("Reload done and now connecting to servers...");
			XServerManager.getInstance().getThreadPool().runTask(new Runnable() {
				public void run() {
					try {
						XServerManager.getInstance().reconnectAll_forced();
					} catch (NotInitializedException e) {
						getPlugin().getLogger().severe(e.getMessage());
					}
				}});
		} catch (NotInitializedException e) {
			sender.sendMessage(ChatColor.RED + "XServer isn't initialized!");
		} catch (IOException e1)
		{
			sender.sendMessage(ChatColor.RED + "XServer ERROR: " + e1.getMessage());
		}
		return true;
	}

}
