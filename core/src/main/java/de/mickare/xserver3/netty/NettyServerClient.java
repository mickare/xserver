package de.mickare.xserver3.netty;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.Service;

import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver3.Client;
import de.mickare.xserver3.NetworkManager;
import de.mickare.xserver3.Server;
import de.mickare.xserver3.netty.handshake.ClientHandshakeHandler.Action;
import de.mickare.xserver3.security.Security;
import io.netty.channel.ChannelFuture;

public class NettyServerClient implements Server, Client {

  private final NettyClient client;
  private final NettyServer server;

  public NettyServerClient(NetworkManager networkManager) {
    this.client = new NettyClient(networkManager);
    this.server = new NettyServer(networkManager);
  }

  @Override
  public NetworkManager getNetworkManager() {
    return server.getNetworkManager();
  }

  @Override
  public Security getSecurity() {
    return server.getSecurity();
  }

  @Override
  public int getServerId() {
    return server.getServerId();
  }

  @Override
  public int getSession() {
    return server.getSession();
  }

  @Override
  public String getName() {
    return server.getName();
  }

  @Override
  public NetworkProto.Server toProtocol() {
    return server.toProtocol();
  }

  @Override
  public void addListener(Listener listener, Executor executor) {
    server.addListener(listener, executor);
  }

  @Override
  public void awaitRunning() {
    server.awaitRunning();
  }

  @Override
  public void awaitRunning(long time, TimeUnit unit) throws TimeoutException {
    server.awaitRunning(time, unit);
  }

  @Override
  public void awaitTerminated() {
    server.awaitTerminated();
  }

  @Override
  public void awaitTerminated(long time, TimeUnit unit) throws TimeoutException {
    server.awaitTerminated(time, unit);
  }

  @Override
  public Throwable failureCause() {
    return server.failureCause();
  }

  @Override
  public boolean isRunning() {
    return server.isRunning();
  }

  @Override
  public Service startAsync() {
    return server.startAsync();
  }

  @Override
  public State state() {
    return server.state();
  }

  @Override
  public Service stopAsync() {
    return server.stopAsync();
  }

  @Override
  public ChannelFuture connect(String ip, int port, Action action) throws InterruptedException {
    return client.connect(ip, port, action);
  }

}
