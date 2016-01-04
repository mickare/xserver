package de.mickare.xserver3;

import java.security.cert.CertificateException;
import java.util.logging.Logger;

import javax.net.ssl.SSLException;

import com.google.common.util.concurrent.AbstractService;

import de.mickare.xserver3.netty.NettyServerClient;
import de.mickare.xserver3.security.NoSecurity;
import lombok.Getter;

public class Core extends AbstractService {

  @Getter
  private NetworkManager manager;

  private NettyServerClient server;

  public Core(Logger logger) {

    try {
      this.manager = new NetworkManager(logger, new NoSecurity());
    } catch (SSLException | CertificateException e) {
      throw new RuntimeException(e);
    }

    server = new NettyServerClient(manager);

  }

  private void onStart() throws Exception {
    this.server.startAsync();
  }

  @Override
  protected void doStart() {
    try {
      this.onStart();
      this.notifyStarted();
    } catch (Throwable t) {
      this.notifyFailed(t);
    }
  }

  private void onStop() throws Exception {

  }

  @Override
  protected void doStop() {
    try {
      this.onStop();
      this.notifyStopped();
    } catch (Throwable t) {
      this.notifyFailed(t);
    }
  }

}
