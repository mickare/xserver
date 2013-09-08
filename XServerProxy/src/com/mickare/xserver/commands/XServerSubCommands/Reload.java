package com.mickare.xserver.commands.XServerSubCommands;


import java.io.IOException;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import com.mickare.xserver.XServerManager;
import com.mickare.xserver.XServerPlugin;
import com.mickare.xserver.commands.SubCommand;
import com.mickare.xserver.exceptions.NotInitializedException;

public class Reload extends SubCommand {

        public Reload(XServerPlugin plugin) {
                super(plugin, "reload", "", "Reloads the server list and reconnects.");
                // TODO Auto-generated constructor stub
        }
        
        @Override
        public void execute(CommandSender sender, String[] args) {
                try {
                	getPlugin().getManager().reload();
                        sender.sendMessage("Reload done and now connecting to servers...");
                        getPlugin().getManager().getThreadPool().runTask(new Runnable() {
                                public void run() {
                                        try {
                                                XServerManager.getInstance().reconnectAll_forced();
                                        } catch (NotInitializedException e) {
                                                getPlugin().getLogger().severe(e.getMessage());
                                        }
                                }});
                } catch (IOException e1)
                {
                        sender.sendMessage(ChatColor.RED + "XServer ERROR: " + e1.getMessage());
                }
                return;
        }

}