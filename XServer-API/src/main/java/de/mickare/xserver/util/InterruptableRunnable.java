package de.mickare.xserver.util;

import de.mickare.xserver.ServerThreadPoolExecutor;

public abstract class InterruptableRunnable implements Runnable {
  private volatile boolean interrupted = false;
  private final String name;

  public InterruptableRunnable(String name) {
    this.name = name;
  }

  public void start(ServerThreadPoolExecutor threadpool) {
    threadpool.runTask(this);
  }

  public boolean isInterrupted() {
    return interrupted;
  }

  public void interrupt() {
    this.interrupted = true;
  }

  public String getName() {
    return name;
  }

}