package de.mickare.xserver3.netty;

import de.mickare.xserver.protocol.ErrorProto.ErrorMessage;
import de.mickare.xserver.protocol.HandshakeProto;
import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver.protocol.TransportProto;
import de.mickare.xserver.protocol.TransportProto.CloseMessage;
import de.mickare.xserver.protocol.TransportProto.Packet;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public final class PacketUtil {

  private PacketUtil() {}


  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final TransportProto.Packet packet) {
    return ctx.writeAndFlush(packet);
  }


  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final TransportProto.Packet.Builder builder) {
    return writeAndFlush(ctx, builder.build());
  }

  /*
   * public static final ChannelFuture write(final ChannelHandlerContext ctx, final
   * TransportProto.Packet packet) { return ctx.write(packet); }
   * 
   * 
   * public static final ChannelFuture write(final ChannelHandlerContext ctx, final
   * TransportProto.Packet.Builder builder) { return write(ctx, builder.build()); }
   */

  // ******************************************************************************
  // Util
  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final ErrorMessage.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setError(value));
  }

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final CloseMessage.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setClose(value));
  }


  // ******************************************************************************
  // Handshake
  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final HandshakeProto.Login.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setLogin(value));
  }

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final HandshakeProto.AuthChallenge.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setAuthChallenge(value));
  }

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final HandshakeProto.AuthResponse.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setAuthResponse(value));
  }

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final HandshakeProto.AuthSuccess.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setAuthSuccess(value));
  }

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final HandshakeProto.ActionRequest.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setActionRequest(value));
  }

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final HandshakeProto.ActionResponse.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setActionResponse(value));
  }

  // ******************************************************************************
  // Transport
  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final TransportProto.Broadcast.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setBroadcast(value));
  }

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final TransportProto.Message.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setMessage(value));
  }

  // ******************************************************************************
  // Network

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final NetworkProto.NetworkInformation.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setNetInfo(value));
  }

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final NetworkProto.Connection.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setConnection(value));
  }

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final NetworkProto.Disconnection.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setDisconnection(value));
  }

  public static final ChannelFuture writeAndFlush(final ChannelHandlerContext ctx,
      final NetworkProto.Heartbeat.Builder value) {
    return writeAndFlush(ctx, Packet.newBuilder().setHeartbeat(value));
  }

}
