package de.mickare.xserver3.netty.handshake;

import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver.protocol.ErrorProto.ErrorMessage;
import de.mickare.xserver.protocol.HandshakeProto.ActionRequest;
import de.mickare.xserver.protocol.HandshakeProto.ActionResponse;
import de.mickare.xserver.protocol.HandshakeProto.AuthChallenge;
import de.mickare.xserver.protocol.HandshakeProto.AuthResponse;
import de.mickare.xserver.protocol.HandshakeProto.AuthSuccess;
import de.mickare.xserver.protocol.HandshakeProto.Login;
import de.mickare.xserver.protocol.TransportProto.Packet;
import de.mickare.xserver3.exception.ConnectionException;
import de.mickare.xserver3.exception.ProtocolException;
import de.mickare.xserver3.netty.PipeUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public abstract class HandshakeHandler extends SimpleChannelInboundHandler<Packet> {

  @Getter
  private static final String name = "auth";

  @RequiredArgsConstructor
  public static enum State {
    NEW(0), AUTH(1), ACTION(2), SUCCESS(3), FAILED(3);
    @Getter
    private final int step;
  }

  private volatile State state = State.NEW;

  private final ChannelPromise promise;

  @Getter
  private ChannelHandlerContext channelContext = null;

  public HandshakeHandler(Channel channel) {
    this.promise = channel.newPromise();
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    this.checkState(State.NEW);
    Preconditions.checkState(this.channelContext == null);
    this.channelContext = ctx;
  }

  public final ChannelFuture future() {
    return this.promise;
  }

  protected final ChannelPromise promise() {
    return this.promise;
  }

  public final State state() {
    return this.state;
  }

  protected final void setState(final State state) throws ProtocolException {
    Preconditions.checkArgument(state != State.FAILED);
    if (this.state.step < state.step) {
      throw new ProtocolException("Can only increase state step!");
    }
    this.state = state;
  }

  protected final Throwable fail(Throwable cause) throws ProtocolException, IllegalStateException {
    if (this.state.step < State.FAILED.step) {
      throw new ProtocolException("Can only increase state step!");
    }
    this.promise.setFailure(cause);
    this.state = State.FAILED;
    return cause;
  }

  protected final void checkState(final State setpoint) throws ConnectionException {
    if (this.promise.isCancelled()) {
      throw new ConnectionException(ErrorMessage.Type.CANCELLED, true, "Handshake cancelled!");
    }
    if (this.state != setpoint) {
      throw new ProtocolException(
          "Wrong state (\"" + this.state.name() + "\" should be \"" + setpoint.name() + "\")!");
    }
    if (this.promise.cause() != null) {
      throw new ProtocolException(this.promise.cause());
    }
  }

  protected final Logger logger(final ChannelHandlerContext ctx) {
    return PipeUtils.getLogger(ctx);
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final Packet msg) throws Exception {
    try {
      switch (msg.getValueCase()) {
        case ERROR:
          handle(ctx, msg.getError());
          break;
        case LOGIN:
          handle(ctx, msg.getLogin());
          break;
        case AUTHCHALLENGE:
          handle(ctx, msg.getAuthChallenge());
          break;
        case AUTHRESPONSE:
          handle(ctx, msg.getAuthResponse());
          break;
        case AUTHSUCCESS:
          handle(ctx, msg.getAuthSuccess());
          break;
        case ACTIONREQUEST:
          handle(ctx, msg.getActionRequest());
          break;
        case ACTIONRESPONSE:
          handle(ctx, msg.getActionResponse());
          break;
        default:
          throw new ProtocolException("Invalid or unknown packet!");
      }
    } catch (final Exception e) {
      if (this.state != State.FAILED && this.state != State.SUCCESS) {
        this.fail(e);
      }
      throw e;
    }
  }

  protected abstract void handle(ChannelHandlerContext ctx, ErrorMessage error) throws Exception;


  protected abstract void handle(ChannelHandlerContext ctx, Login login) throws Exception;


  protected abstract void handle(ChannelHandlerContext ctx, AuthChallenge authChallenge)
      throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, AuthResponse authResponse)
      throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, AuthSuccess authSuccess)
      throws Exception;


  protected abstract void handle(ChannelHandlerContext ctx, ActionRequest actionRequest)
      throws Exception;

  protected abstract void handle(ChannelHandlerContext ctx, ActionResponse actionResponse)
      throws Exception;

  public abstract NetworkProto.Server getProtocolServer();

  public abstract NetworkProto.Server getProtocolClient();

}
