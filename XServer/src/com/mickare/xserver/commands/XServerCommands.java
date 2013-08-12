package com.mickare.xserver.commands;

import java.util.Arrays;
import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.XServerSubCommands.*;

public class XServerCommands extends AbstractCommand {

	private HashSet<SubCommand> commands = new HashSet<SubCommand>();
	
	public XServerCommands(XServerPlugin plugin) {
		super(plugin, "xserver");
		initialize();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("hilfe") || args[0].equals("?")) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.GOLD);
				sb.append("XServer Sub-Commands:");
				for (SubCommand sc : commands) {
					sb.append("\n").append(ChatColor.RESET).append(" - ").append(sc.getCommand())
							.append(ChatColor.GRAY).append(" | ").append(sc.getDescription());
				}
				sender.sendMessage(sb.toString());
				return true;
			}
	
			if (getSubCommand(args[0]) != null) {
				return getSubCommand(args[0]).onCommand(sender, cmd, label, Arrays.copyOfRange(args, 1, args.length));
			}
			sender.sendMessage(ChatColor.DARK_RED + "No valid Sub Command.");
		}
		return false;
	}

	
	public HashSet<SubCommand> getSubCommands() {
		return commands;
	}
	
	public SubCommand getSubCommand(String cmdstr) {
		for (SubCommand cmd : commands) {
			if (cmd.getCommand().equalsIgnoreCase(cmdstr)) {
				return cmd;
			}
		}
		return null;
	}
	
	private void initialize() {
		commands.add(new PingCommand(getPlugin()));
		commands.add(new Reconnect(getPlugin()));
		commands.add(new Reload(getPlugin()));
		commands.add(new Status(getPlugin()));
	}


}
