package de.mickare.xserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import de.mickare.xserver.net.ConnectionObj;

public class MainServer implements Runnable {

  private final ServerSocket server;
  private final AbstractXServerManagerObj manager;
  private final AtomicReference<Future<?>> task = new AtomicReference<Future<?>>(null);

  protected MainServer(ServerSocket server, AbstractXServerManagerObj manager) {
    // super( "XServer Main Server Thread" );
    this.server = server;
    this.manager = manager;
  }

  public synchronized void close() throws IOException {
    try {
      this.server.close();
    } finally {
      if (this.task.get() != null) {
        this.task.get().cancel(true);
        this.task.set(null);
      }
    }
  }

  public boolean isClosed() {
    return this.server.isClosed();
  }

  @Override
  public void run() {
    while (!isClosed()) {
      try {
        new ConnectionObj(server.accept(), manager);
      } catch (SocketTimeoutException ste) {
        // ignore
      } catch (IOException e) {
        if (!isClosed() || !"Socket closed".equals(e.getMessage()) ) {
          // Only log if not shutdown. This prevents misleading issue reports on github.
          manager.getLogger().log(Level.WARNING, "Exception while client connects: " + e.getMessage(), e);
        }
      }
    }
  }

  public synchronized void start(ServerThreadPoolExecutor stpool) {
    if (task.get() == null) {
      task.set(stpool.runServerTask(this));
    }
  }

}
