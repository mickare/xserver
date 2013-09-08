package com.mickare.xserver.commands.XServerSubCommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.mickare.xserver.BukkitXServerPlugin;
import com.mickare.xserver.commands.SubCommand;

public class StressTest extends SubCommand {

	public StressTest(BukkitXServerPlugin plugin) {
		super(plugin, "stresstest", "", "Stress test for XServer.");
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		for(int x = 0; x < 40; x++) {
			getPlugin().getServer().dispatchCommand(sender, "scommand all xserver ping");
		}
		return true;
	}

}
