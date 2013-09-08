package com.mickare.xserver.user;

import org.bukkit.command.CommandSender;

public class BukkitComSender implements ComSender {

	private final CommandSender sender;
	
	public BukkitComSender(CommandSender sender) {
		this.sender = sender;
	}
	
	public CommandSender getSender() {
		return sender;
	}
	
	@Override
	public String getName() {
		return sender.getName();
	}

	@Override
	public boolean hasPermission(String perm) {
		return sender.hasPermission(perm);
	}

	@Override
	public void sendMessage(String message) {
		sender.sendMessage(message);
	}

}
