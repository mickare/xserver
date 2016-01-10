package de.mickare.xserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.net.ServerSocketFactory;

import de.mickare.xserver.net.ConnectionObj;

public class MainServer {

  public final static int SOCKET_TIMEOUT = 1000;

  private final AbstractXServerManagerObj manager;
  private final ServerSocket socket;
  private volatile Future<?> task = null;
  private volatile boolean running = false;

  protected MainServer(int port, AbstractXServerManagerObj manager) throws IOException {

    socket = ServerSocketFactory.getDefault().createServerSocket();
    socket.setReuseAddress(true);
    socket.setPerformancePreferences(0, 1, 1);
    socket.setSoTimeout(SOCKET_TIMEOUT);
    socket.bind(new InetSocketAddress(port), 500);

    this.manager = manager;
  }

  public final synchronized void stop() throws IOException {
    if (running) {
      this.manager.debugInfo("Stopping MainServer...");
      running = false;

      if (this.task != null) {
        this.task.cancel(true);
      }

      this.socket.close();
    }
  }

  public final boolean isRunning() {
    return running && !this.socket.isClosed();
  }

  protected final void run() {
    this.manager.debugInfo("MainServer started");
    while (isRunning()) {
      try {
        final Socket temp = socket.accept();

        try {
          if (isRunning()) {
            new ConnectionObj(temp, manager);
          } else {
            temp.close();
          }
        } catch (Exception e) {

          try {
            temp.close();
          } catch (IOException ie) {
          }

          throw e;
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

  public final synchronized MainServer start(final ServerThreadPoolExecutor stpool) {
    if (!running && task == null) {
      this.manager.debugInfo("Starting MainServer...");
      running = true;
      this.task = stpool.runServerTask(new Runnable() {
        public void run() {
          MainServer.this.run();
        }
      });
    }
    return this;
  }

}
