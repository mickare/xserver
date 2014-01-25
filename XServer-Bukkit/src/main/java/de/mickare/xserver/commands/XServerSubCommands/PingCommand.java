package de.mickare.xserver.commands.XServerSubCommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import de.mickare.xserver.BukkitXServerPlugin;
import de.mickare.xserver.commands.SubCommand;
import de.mickare.xserver.net.Ping;
import de.mickare.xserver.net.PingObj;
import de.mickare.xserver.net.XServer;
import de.mickare.xserver.user.BukkitComSender;

public class PingCommand extends SubCommand {

	public PingCommand(BukkitXServerPlugin plugin) {
		super(plugin, "ping", "[Servername]", "Ping all servers, or the one as given.");
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {

			if(args.length > 0) {
				XServer s = getPlugin().getManager().getServer(args[0]);
				if(s == null) {
					sender.sendMessage(ChatColor.RED + "Server \"" + args[0] + "\" not found!");
					return true;
				}
				Ping p = new PingObj(getPlugin().getManager(), new BukkitComSender(sender), getPlugin().getManager().homeServer.getName());
				p.add(s);
				p.start();
			} else {
				
				Ping p = new PingObj(getPlugin().getManager(), new BukkitComSender(sender), getPlugin().getManager().homeServer.getName());
				p.addAll(getPlugin().getManager().getServers());
				p.start();
				
			}

		return true;
	}

}
