package de.mickare.xserver.commands.XServerSubCommands;


import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import de.mickare.xserver.XServerPlugin;
import de.mickare.xserver.commands.SubCommand;
import de.mickare.xserver.net.Ping;
import de.mickare.xserver.net.PingObj;
import de.mickare.xserver.net.XServer;
import de.mickare.xserver.user.BungeeComSender;

public class PingCommand extends SubCommand {

        public PingCommand(XServerPlugin plugin) {
                super(plugin, "ping", "[Servername]", "Ping all servers, or the one as given.");
                // TODO Auto-generated constructor stub
        }

        @SuppressWarnings("deprecation")
		@Override
        public void execute(CommandSender sender, String[] args) {
                        if(args.length > 0) {
                                XServer s = getPlugin().getManager().getServer(args[0]);
                                if(s == null) {
                                        sender.sendMessage(ChatColor.RED + "Server \"" + args[0] + "\" not found!");
                                        return;
                                }
                                Ping p = new PingObj(getPlugin().getManager(), new BungeeComSender(sender), getPlugin().getManager().getHomeServer().getName());
                                p.add(s);
                                p.start();
                        } else {
                                
                                Ping p = new PingObj(getPlugin().getManager(), new BungeeComSender(sender), getPlugin().getManager().getHomeServer().getName());
                                p.addAll(getPlugin().getManager().getServers());
                                p.start();
                                
                        }
                return;
        }

}