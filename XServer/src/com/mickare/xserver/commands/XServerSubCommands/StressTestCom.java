package com.mickare.xserver.commands.XServerSubCommands;

import java.io.IOException;
import java.util.LinkedList;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.BukkitXServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.net.XServer;
import com.mickare.xserver.stresstest.StressTest;
import com.mickare.xserver.user.BukkitComSender;
import com.mickare.xserver.util.ChatColor;

public class StressTestCom extends SubCommand {

	public StressTestCom(BukkitXServerPlugin plugin) {
		super(plugin, "stresstest", "<times = 10> <targets = all> <async = false>", "Stress test for XServer. Targets can be: (all|servernames splitted by \";\")");
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		//Standards
		int times = 10;
		String[] targets = {"all"};
		boolean async = true;
		
		if(args.length > 0) {
			try {
				times = Integer.parseInt(args[0]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(ChatColor.RED + "Wrong Number Format for times!");
				return true;
			}
		}
		
		if(args.length > 1) {
			targets = args[1].split(";");
		}
		
		LinkedList<XServer> targetServers = new LinkedList<XServer>();
		if(targets.length == 1) {
			XServer st = getPlugin().getManager().getServer(targets[0]);
			if(st != null) {
				targetServers.add(st);
			} else if(targets[0].equalsIgnoreCase("all")) {
				targetServers.addAll(getPlugin().getManager().getServers());
			} else {
				sender.sendMessage(ChatColor.RED + "Server \"" + targets[0] + "\" not found!");
				return true;
			}
		} else {
			for(String t : targets) {
				XServer st = getPlugin().getManager().getServer(t);
				if(st != null) {
					targetServers.add(st);
				}
			}
		}
		
		if(targetServers.isEmpty()) {
			sender.sendMessage(ChatColor.RED + "Servers not found!");
			return true;
		}
		
		if(args.length > 2) {
			async = Boolean.parseBoolean(args[2]);
		}
		
		StressTest st = new StressTest(getPlugin().getManager(), new BukkitComSender(sender), times, sender.getName(), !async);
		st.addAll(targetServers);
		try {
			st.start();
		} catch (IOException e) {
			sender.sendMessage(ChatColor.RED + "An Exception occured! (" + e.getMessage() + ")");
			return true;
		}
		
		return true;
	}

}
