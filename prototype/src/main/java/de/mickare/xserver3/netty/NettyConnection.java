package de.mickare.xserver3.netty;

import java.util.concurrent.Future;

import com.google.common.base.Preconditions;

import de.mickare.xserver.protocol.ErrorProto;
import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver.protocol.TransportProto.Broadcast;
import de.mickare.xserver.protocol.TransportProto.CloseMessage;
import de.mickare.xserver.protocol.TransportProto.Message;
import de.mickare.xserver.protocol.TransportProto.Packet;
import de.mickare.xserver3.netty.handshake.ClientHandshakeHandler;
import de.mickare.xserver3.netty.handshake.ServerHandshakeHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;


public class NettyConnection {

  @Getter
  private final boolean master;

  @Getter
  private final NetworkProto.Server server, client;

  private final ChannelHandlerContext ctx;


  @Getter
  private MainHandler mainHandler;

  private NettyConnection(boolean selfIsMaster, NetworkProto.Server server,
      NetworkProto.Server client, ChannelHandlerContext ctx) {
    Preconditions.checkNotNull(server);
    Preconditions.checkNotNull(client);
    Preconditions.checkNotNull(ctx);
    this.master = selfIsMaster;
    this.server = server;
    this.client = client;
    this.ctx = ctx;
    this.mainHandler = new MainHandler(this);
  }

  public NetworkProto.Server getPartner() {
    if (master) {
      return this.client;
    } else {
      return this.server;
    }
  }

  public NettyConnection(ServerHandshakeHandler handler) {
    this(true, handler.getProtocolServer(), handler.getProtocolClient(),
        handler.getChannelContext());
  }


  public NettyConnection(ClientHandshakeHandler handler) {
    this(false, handler.getProtocolServer(), handler.getProtocolClient(),
        handler.getChannelContext());
  }

  public boolean isOpen() {
    return this.ctx.channel().isOpen();
  }

  public Future<Void> closeNormal() {
    return closeNormal("no reason");
  }

  public Future<Void> closeNormal(String reason) {
    Preconditions.checkNotNull(reason);
    PacketUtil.writeAndFlush(ctx, CloseMessage.newBuilder().setNormal(reason));
    return this.ctx.close();
  }

  public Future<Void> closeReplaced(NetworkProto.Server superseder) {
    Preconditions.checkNotNull(superseder);
    PacketUtil.writeAndFlush(ctx, CloseMessage.newBuilder().setReplaced(superseder));
    return this.ctx.close();
  }

  public Future<Void> closeError(ErrorProto.ErrorMessage error) {
    Preconditions.checkNotNull(error);
    PacketUtil.writeAndFlush(ctx, CloseMessage.newBuilder().setError(error));
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
    return "Connection [" + toString(server) + ":" + " -> " + toString(client) + "]";
  }

  private String toString(NetworkProto.Server server) {
    return server.getId() + ":" + server.getName();
  }

}
