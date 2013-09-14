package com.mickare.xserver.commands.XServerSubCommands;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.BukkitXServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.net.XServer;

public class PackageCommand extends SubCommand {

	public PackageCommand(BukkitXServerPlugin plugin) {
		super(plugin, "package", "[Servername]", "Shows package speed to all servers, or the one as given.");
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if(args.length > 0) {
			XServer s = getPlugin().getManager().getServer(args[0]);
			if(s == null) {
				sender.sendMessage(ChatColor.RED + "Server \"" + args[0] + "\" not found!");
				return true;
			}
			
			StringBuilder sb = new StringBuilder();
			appendHeaderLine(sb);
			appendSpeedLine(sb.append("\n"), s);
			sender.sendMessage(sb.toString());
			
		} else {
			
			List<XServer> servers = new LinkedList<XServer>(getPlugin().getManager().getServers());
			
			Collections.sort(servers, new Comparator<XServer>() {
				@Override
				public int compare(XServer o1, XServer o2)
				{
					return o1.getName().compareTo(o2.getName());
				}
			});
			
			StringBuilder sb = new StringBuilder();
			appendHeaderLine(sb);
			for(XServer s : servers) {
				appendSpeedLine(sb.append("\n"), s);
			}
			sender.sendMessage(sb.toString());
		}
		
		return true;
	}

	private StringBuilder appendHeaderLine(StringBuilder sb) {
		sb.append(ChatColor.GRAY).append("Packages per second\n")
		.append("Name  ").append("Sending").append(":").append("Receiving").append(" (").append("RecordSending").append(":").append("RecordReceiving").append(")");
		return sb;
	}

	private StringBuilder appendSpeedLine(StringBuilder sb, XServer s) {
		sb.append(s.isConnected() ? ChatColor.GREEN : ChatColor.RED).append(s.getName()).append("  ");

		if (s.isConnected()) {

			sb.append(ChatColor.GOLD).append(s.getSendinglastSecondPackageCount()).append(ChatColor.GRAY).append(":")
					.append(ChatColor.GOLD).append(s.getReceivinglastSecondPackageCount())

					.append(ChatColor.GRAY).append("  (").append(ChatColor.AQUA)
					.append(s.getSendingRecordSecondPackageCount()).append(ChatColor.GRAY).append(":")
					.append(ChatColor.AQUA).append(s.getReceivingRecordSecondPackageCount()).append(ChatColor.GRAY)
					.append(")");
		} else {
			sb.append(ChatColor.GRAY).append(" - not connected!");
		}
		return sb;
	}

}
