package de.mickare.xserver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class BungeeServerThreadPool implements ServerThreadPoolExecutor {

  // private final BungeeXServerPlugin plugin;
  private final ExecutorService es;

  public BungeeServerThreadPool(BungeeXServerPlugin plugin) {
    // this.plugin = plugin;
    es = plugin.getProxy().getScheduler().unsafe().getExecutorService(plugin);
  }

  @Override
  public Future<?> runTask(Runnable task) {
    return es.submit(task);
  }

  @Override
  public void shutDown() {
    // TODO Auto-generated method stub

  }

  @Override
  public Future<?> runServerTask(Runnable task) {
    return es.submit(task);
  }

}
