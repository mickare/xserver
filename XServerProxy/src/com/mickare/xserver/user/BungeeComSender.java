package com.mickare.xserver.user;

import net.md_5.bungee.api.CommandSender;

public class BungeeComSender implements ComSender {

	
	private final CommandSender sender;
	
	public BungeeComSender(CommandSender sender) {
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
