package com.mickare.xserver;

import org.bukkit.plugin.java.JavaPlugin;

public class BukkitEventHandler extends EventHandler<JavaPlugin> {

	private final JavaPlugin plugin;

	protected BukkitEventHandler(JavaPlugin plugin) {
		super(plugin.getLogger());
		this.plugin = plugin;
	}

	@Override
	protected void runTask(Boolean sync, XServerListenerPlugin<JavaPlugin> plugin, Runnable run) {
		boolean s = true;
		if (sync != null) {
			s = sync.booleanValue();
		}
		if (plugin.getPlugin().isEnabled()) {
			if (s) {
				plugin.getPlugin().getServer().getScheduler().runTask(plugin.getPlugin(), run);
			} else {
				plugin.getPlugin().getServer().getScheduler()
						.runTaskAsynchronously(plugin.getPlugin(), run);
			}
		}
	}

	public JavaPlugin getPlugin() {
		return plugin;
	}


}
