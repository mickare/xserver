package com.mickare.xserver.commands;


import net.md_5.bungee.api.CommandSender;

import com.mickare.xserver.XServerPlugin;

public abstract class SubCommand<T> {

        private final XServerPlugin<T> plugin;

        private final String command, arguments, desc;

        public SubCommand(XServerPlugin<T> plugin, String command, String arguments, String desc) {
                this.plugin = plugin;
                this.command = command;
                this.arguments = arguments;
                this.desc = desc;
        }

        public abstract void execute(CommandSender sender, String[] args);

        public XServerPlugin<T> getPlugin() {
                return plugin;
        }

        public String getCommand() {
                return command;
        }

        public String getDescription() {
                return desc;
        }

        public String getArguments() {
                return arguments;
        }

}