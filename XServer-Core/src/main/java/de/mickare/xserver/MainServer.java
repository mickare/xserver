package de.mickare.xserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import de.mickare.xserver.net.ConnectionObj;

public class MainServer {

  private final AbstractXServerManagerObj manager;
  private final ServerSocket server;  
  private volatile Future<?> task = null;
  private volatile boolean running = false;

  protected MainServer(ServerSocket server, AbstractXServerManagerObj manager) {
    this.server = server;
    this.manager = manager;
  }

  public final synchronized void stop() throws IOException {
    if (running) {
      this.manager.debugInfo("Stopping MainServer...");
      running = false;

      if (this.task != null) {
        this.task.cancel(true);
      }

      this.server.close();
    }
  }

  public final boolean isRunning() {
    return running && !this.server.isClosed();
  }

  protected final void run() {
    this.manager.debugInfo("MainServer started");
    while (isRunning()) {
      try {
        final Socket socket = server.accept();
        if (isRunning()) {
          new ConnectionObj(socket, manager);
        } else {
          socket.close();
        }
      } catch (SocketTimeoutException ste) {
        // ignore
      } catch (SocketException e) {
        if (running || !"socket closed".equals(e.getMessage())) {
          manager.getLogger().log(Level.WARNING, "Exception while client connects: " + e.getMessage(), e);
        }
      } catch (IOException e) {
        // Only log if not shutdown. This prevents misleading issue reports on github.
        manager.getLogger().log(Level.WARNING, "Exception while client connects: " + e.getMessage(), e);
      }
    }
    this.manager.debugInfo("MainServer stopped");
  }

  public final synchronized void start(final ServerThreadPoolExecutor stpool) {
    if (!running && task == null) {
      this.manager.debugInfo("Starting MainServer...");
      running = true;
      this.task = stpool.runServerTask(new Runnable() {
        public void run() {
          MainServer.this.run();
        }
      });
    }
  }

}
