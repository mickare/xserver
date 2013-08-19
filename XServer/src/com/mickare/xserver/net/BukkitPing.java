package com.mickare.xserver.net;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class BukkitPing extends Ping {
	
	
	private final CommandSender sender;
	
	public BukkitPing(CommandSender sender) {
		this(sender, "Ping");
	}
	
	public BukkitPing(CommandSender sender, String salt) {
		super(salt);
		this.sender = sender;
	}
	
	public String getFormatedString() {
			if(getWaiting().size() > 0) {
				return "Still Pending...";
			} else {
				StringBuilder sb = new StringBuilder();
				for(Entry<XServer, Long> es : getResponses().entrySet()) {
					sb.append("\n").append(ChatColor.GOLD).append(es.getKey().getName()).append(ChatColor.GRAY).append(" - ");
					if(es.getValue() < 0) {
						sb.append(ChatColor.RED).append("Not connected!");
					} else if(es.getValue() == Long.MAX_VALUE) {
						if(es.getKey().isConnected()) {
							sb.append(ChatColor.RED).append("Timeout!");
						} else {
							sb.append(ChatColor.RED).append("Timeout! Connection lost!");
						}
					} else {
						long diff = es.getValue() - getStarted();
						if(diff < 10) {
							sb.append(ChatColor.GREEN);
						} else if (diff < 30) {
							sb.append(ChatColor.YELLOW);
						} else if (diff < 100) {
							sb.append(ChatColor.GOLD);
						} else {
							sb.append(ChatColor.RED);
						}
						sb.append(diff).append("ms");
					}
				}
				return sb.toString();
			}

	}

	public CommandSender getSender() {
		return sender;
	}

	@Override
	protected void sendMessageToCommandSender(String message) {
		sender.sendMessage(message);
	}


	
}
