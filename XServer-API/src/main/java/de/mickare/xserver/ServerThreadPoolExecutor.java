package de.mickare.xserver;

import java.util.concurrent.Future;

public interface ServerThreadPoolExecutor {

  /**
   * Here we add our jobs to working queue
   *
   * @param task a Runnable task
   */
  public abstract Future<?> runTask(Runnable task);

  /**
   * Shutdown the Threadpool if it's finished
   */
  public abstract void shutDown();

  /**
   * Run tha main ServerSocket Task
   * 
   * @param task
   * @return
   */
  Future<?> runServerTask(Runnable task);

}
