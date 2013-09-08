package com.mickare.xserver.commands.XServerSubCommands;


import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.net.Ping;
import com.mickare.xserver.net.XServer;
import com.mickare.xserver.user.BungeeComSender;

public class PingCommand<T> extends SubCommand<T> {

        public PingCommand(XServerPlugin<T> plugin) {
                super(plugin, "ping", "[Servername]", "Ping all servers, or the one as given.");
                // TODO Auto-generated constructor stub
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
                        if(args.length > 0) {
                                XServer<T> s = getPlugin().getManager().getServer(args[0]);
                                if(s == null) {
                                        sender.sendMessage(ChatColor.RED + "Server \"" + args[0] + "\" not found!");
                                        return;
                                }
                                Ping<T> p = new Ping<T>(getPlugin().getManager(), new BungeeComSender(sender), getPlugin().getManager().homeServer.getName());
                                p.add(s);
                                p.start();
                        } else {
                                
                                Ping<T> p = new Ping<T>(getPlugin().getManager(), new BungeeComSender(sender), getPlugin().getManager().homeServer.getName());
                                p.addAll(getPlugin().getManager().getServers());
                                p.start();
                                
                        }
                return;
        }

}