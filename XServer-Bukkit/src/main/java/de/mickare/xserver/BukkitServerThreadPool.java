package de.mickare.xserver;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.bukkit.Bukkit;

// public class BukkitServerThreadPool extends ThreadPoolExecutor implements
// ServerThreadPoolExecutor {
public class BukkitServerThreadPool implements ServerThreadPoolExecutor {

  private final BukkitXServerPlugin bukkitPlugin;

  private volatile boolean stopped = false;

  public BukkitServerThreadPool(BukkitXServerPlugin bukkitPlugin) {
    this.bukkitPlugin = bukkitPlugin;
  }

  @Override
  public Future<?> runTask(Runnable task) {
    if (stopped) {
      return null;
    }    
    FutureTask<?> f = new FutureTask<Object>(task, new Object());
    Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, f);
    return f;
  }

  @Override
  public void shutDown() {
    stopped = true;
  }

  @Override
  public Future<?> runServerTask(final Runnable task) {
    if (stopped) {
      return null;
    }
    FutureTask<?> f = new FutureTask<Object>(task, new Object());
    final Thread t = new Thread(f, "XServer Server Thread");
    t.start();
    return f;
  }

}
