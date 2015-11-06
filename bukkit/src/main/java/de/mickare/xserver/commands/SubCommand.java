package de.mickare.xserver.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import de.mickare.xserver.BukkitXServerPlugin;

public abstract class SubCommand {

	private final BukkitXServerPlugin plugin;

	private final String command, arguments, desc;

	public SubCommand(BukkitXServerPlugin plugin, String command, String arguments, String desc) {
		this.plugin = plugin;
		this.command = command;
		this.arguments = arguments;
		this.desc = desc;
	}

	public abstract boolean onCommand(CommandSender sender, Command cmd, String label, String[] args);

	public BukkitXServerPlugin getPlugin() {
		return plugin;
	}

	public String getCommand() {
		return command;
	}

	public String getDescription() {
		return desc;
	}

	public String getArguments() {
		return arguments;
	}

}
