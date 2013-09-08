package com.mickare.xserver.commands.XServerSubCommands;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.net.XServer;

public class Status<T> extends SubCommand<T> {

        public Status(XServerPlugin<T> plugin) {
                super(plugin, "status", "",
                                "Shows the connection status of the servers.");
                // TODO Auto-generated constructor stub
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
                StringBuilder sb = new StringBuilder();

                        LinkedList<XServer<T>> servers = new LinkedList<XServer<T>>(
                                        getPlugin().getManager().getServers());

                        Collections.sort(servers, new Comparator<XServer<T>>() {
                                @Override
                                public int compare(XServer<T> o1, XServer<T> o2) {
                                        return o1.getName().compareTo(o2.getName());
                                }
                        });

                        for (XServer<T> s : servers) {
                                sb.append("\n").append(ChatColor.RESET).append(s.getName())
                                                .append(ChatColor.GRAY).append(" : ");
                                if (s.isConnected()) {
                                        sb.append(ChatColor.GREEN).append("connected");
                                } else {
                                        sb.append(ChatColor.RED).append("not connected");
                                }
                        }


                sender.sendMessage(sb.toString());
                return;
        }

}