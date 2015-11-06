package de.mickare.xserver3.netty;

import java.util.concurrent.Future;

import com.google.common.base.Preconditions;

import de.mickare.xserver.protocol.Network;
import de.mickare.xserver.protocol.Transport.Broadcast;
import de.mickare.xserver.protocol.Transport.Message;
import de.mickare.xserver.protocol.Transport.Packet;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.Getter;


public class NettyConnection {

  @Getter
  private final boolean selfIsMaster;

  @Getter
  private final Network.Server master, slave;

  private final ChannelHandlerContext ctx;


  @Getter(AccessLevel.PROTECTED)
  private MainHandler mainHandler;

  public NettyConnection(boolean selfIsMaster, Network.Server master, Network.Server slave,
      ChannelHandlerContext ctx) {
    Preconditions.checkNotNull(master);
    Preconditions.checkNotNull(slave);
    Preconditions.checkNotNull(ctx);
    this.selfIsMaster = selfIsMaster;
    this.master = master;
    this.slave = slave;
    this.ctx = ctx;
    this.mainHandler = new MainHandler(this);
  }

  public Future<Void> close() {
    return this.ctx.close();
  }

  public final void send(final Packet.Builder builder) {
    send(builder.build());
  }

  public final void send(final Packet packet) {
    this.ctx.writeAndFlush(packet);
  }

  public void send(final Message msg) {
    send(Packet.newBuilder().setMessage(msg));
  }

  public void send(final Broadcast msg) {
    send(Packet.newBuilder().setBroadcast(msg));
  }

  public String toString() {
    return "Connection [" + toString(master) + ":" + " -> " + toString(slave) + "]";
  }

  private String toString(Network.Server server) {
    return server.getId() + ":" + server.getName();
  }

}
