package com.mickare.xserver;

import org.bukkit.plugin.java.JavaPlugin;

public class BukkitEventHandler extends EventHandler<JavaPlugin> {

	private final JavaPlugin plugin;

	protected BukkitEventHandler(JavaPlugin plugin) {
		super(plugin.getLogger());
		this.plugin = plugin;
	}

	@Override
	protected void runTask(Boolean sync, JavaPlugin plugin, Runnable run) {
		boolean s = true;
		if (sync != null) {
			s = sync.booleanValue();
		}
		if (plugin.isEnabled()) {
			if (s) {
				plugin.getServer().getScheduler().runTask(plugin, run);
			} else {
				plugin.getServer().getScheduler()
						.runTaskAsynchronously(plugin, run);
			}
		}
	}

	public JavaPlugin getPlugin() {
		return plugin;
	}

}
