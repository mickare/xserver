package de.mickare.xserver;

import org.bukkit.plugin.java.JavaPlugin;

public class BukkitListenerPlugin implements XServerListenerPlugin<JavaPlugin> {

  private final JavaPlugin plugin;

  public BukkitListenerPlugin(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public JavaPlugin getPlugin() {
    return plugin;
  }

}
