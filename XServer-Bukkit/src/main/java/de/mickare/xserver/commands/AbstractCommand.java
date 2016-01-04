package de.mickare.xserver.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import de.mickare.xserver.BukkitXServerPlugin;

public abstract class AbstractCommand implements CommandExecutor {

  private final BukkitXServerPlugin plugin;

  private final String command;

  public AbstractCommand(BukkitXServerPlugin plugin, String command) {
    this.plugin = plugin;
    this.command = command;
    plugin.getCommand(command).setExecutor(this);
  }

  @Override
  public abstract boolean onCommand(CommandSender sender, Command cmd, String label, String[] args);

  public BukkitXServerPlugin getPlugin() {
    return plugin;
  }

  public String getCommand() {
    return command;
  }

  public String getDescription() {
    return plugin.getCommand(command).getDescription();
  }

  public String getUsage() {
    return plugin.getCommand(command).getUsage();
  }

}
