package de.mickare.xserver3;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver.protocol.NetworkProto.Server;
import de.mickare.xserver3.netty.NettyConnection;
import de.mickare.xserver3.netty.handshake.ClientHandshakeHandler;
import de.mickare.xserver3.netty.handshake.HandshakeHandler;
import de.mickare.xserver3.netty.handshake.ServerHandshakeHandler;
import de.mickare.xserver3.network.Network;
import de.mickare.xserver3.security.Security;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Getter;

public class NetworkManager {

  public static final int PROTOCOL_VERSION = 1;

  @Getter
  private final Logger logger;

  @Getter
  private int maxConnections = 12;

  @Getter
  private final Security security;

  private final SettableFuture<Integer> serverId = SettableFuture.create();
  @Getter
  private Optional<String> name = Optional.absent();
  @Getter
  private final int session = (int) (0xFFFFFFFF & System.currentTimeMillis());
  @Getter
  private Optional<String> ip = Optional.absent();
  @Getter
  private Optional<Integer> port = Optional.absent();

  @Getter
  private final Network network = new Network(this, null);

  private final Map<Integer, NettyConnection> connections = new ConcurrentHashMap<>();

  @Getter
  private final EventLoopGroup workerGroup;

  public NetworkManager(Logger logger, Security security) {
    Preconditions.checkNotNull(logger);
    this.logger = logger;
    this.security = security;
    this.workerGroup = new NioEventLoopGroup();
  }

  public NettyConnection connected(HandshakeHandler handler) {
    NettyConnection con = null;
    if (handler instanceof ServerHandshakeHandler) {
      con = new NettyConnection((ServerHandshakeHandler) handler);
    } else if (handler instanceof ClientHandshakeHandler) {
      con = new NettyConnection((ClientHandshakeHandler) handler);
    } else {
      throw new IllegalArgumentException("Unknown Handler type!");
    }

    NettyConnection old = connections.put(con.getPartner().getId(), con);
    if (old != null) {
      old.closeReplaced(con.getPartner());
    }
    return con;
  }

  public int getServerId() {
    if (this.serverId.isDone()) {
      try {
        return this.serverId.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
    return 0;
  }

  public ListenableFuture<Integer> getServerIdFuture() {
    return this.serverId;
  }

  public boolean setServerId(int id) {
    return this.serverId.set(id);
  }

  public void setName(String name) {
    this.name = Optional.fromNullable(name);
  }

  public void setIp(String ip) {
    this.ip = Optional.fromNullable(ip);
  }

  public synchronized int setPort(int port) {
    this.port = Optional.of(port);
    return port;
  }

  public synchronized int getPortOrRandom() {
    if (this.port.isPresent()) {
      return port.get();
    }
    ServerSocket s = null;
    try {
      s = ServerSocketFactory.getDefault().createServerSocket();
      return setPort(s.getLocalPort());
    } catch (IOException e) {
    } finally {
      if (s != null) {
        try {
          s.close();
        } catch (IOException e) {
        }
      }
    }
    return setPort(new Random().nextInt(10000) + 4000);
  }

  public EventLoopGroup createServerGroup() {
    return new NioEventLoopGroup(1);
  }

  public NetworkProto.Server getProtocolServer() {
    NetworkProto.Server.Builder b = NetworkProto.Server.newBuilder();
    b.setId(this.getServerId());
    b.setSession(this.getSession());
    if (this.name.isPresent())
      b.setName(this.name.get());
    if (this.ip.isPresent())
      b.setName(this.ip.get());
    if (this.port.isPresent())
      b.setPort(this.port.get());
    b.setProtocolVersion(NetworkManager.PROTOCOL_VERSION);
    b.setMaxConnections(this.maxConnections);
    return b.build();
  }

  public void connectForwardTo(Server server, List<Server> forwards) {
    List<Server> tmp = Lists.newArrayListWithExpectedSize(forwards.size());
    forwards.stream().filter(s -> s.getId() != 0 && s.getId() != server.getId()).forEach(tmp::add);
    Collections.sort(tmp, (a, b) -> 1);

  }

}
