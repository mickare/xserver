package com.mickare.xserver.commands;

import java.util.Arrays;
import java.util.HashSet;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;


import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.XServerSubCommands.*;

public class XServerCommands<T> extends Command {

        private HashSet<SubCommand<T>> commands = new HashSet<SubCommand<T>>();
        
        private final XServerPlugin<T> plugin;
        
        public XServerCommands(XServerPlugin<T> plugin) {
                super("xserver", "xserver");
                this.plugin = plugin;
                initialize();
        }
        
        public void  execute(CommandSender sender, String[] args) {
                if (args.length > 0) {
                        if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("hilfe") || args[0].equals("?")) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(ChatColor.GOLD);
                                sb.append("XServer Sub-Commands:");
                                for (SubCommand<T> sc : commands) {
                                        sb.append("\n").append(ChatColor.RESET).append(" - ").append(sc.getCommand())
                                                        .append(ChatColor.GRAY).append(" | ").append(sc.getDescription());
                                }
                                sender.sendMessage(sb.toString());
                                return;
                        }
        
                        if (getSubCommand(args[0]) != null) {
                                getSubCommand(args[0]).execute(sender, Arrays.copyOfRange(args, 1, args.length));
                                return;
                        }
                        sender.sendMessage(ChatColor.DARK_RED + "No valid Sub Command.");
                }
                return;
        }

        
        public HashSet<SubCommand<T>> getSubCommands() {
                return commands;
        }
        
        public SubCommand<T> getSubCommand(String cmdstr) {
                for (SubCommand<T> cmd : commands) {
                        if (cmd.getCommand().equalsIgnoreCase(cmdstr)) {
                                return cmd;
                        }
                }
                return null;
        }
        
        private void initialize() {
                commands.add(new PingCommand<T>(plugin));
                commands.add(new Reconnect<T>(plugin));
                commands.add(new Reload<T>(plugin));
                commands.add(new Status<T>(plugin));
        }


}