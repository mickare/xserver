package com.mickare.xserver.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.XServerPlugin;

public abstract class SubCommand {

	private final XServerPlugin plugin;

	private final String command, arguments, desc;

	public SubCommand(XServerPlugin plugin, String command, String arguments, String desc) {
		this.plugin = plugin;
		this.command = command;
		this.arguments = arguments;
		this.desc = desc;
	}

	public abstract boolean onCommand(CommandSender sender, Command cmd, String label, String[] args);

	public XServerPlugin getPlugin() {
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
