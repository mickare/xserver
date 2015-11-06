package de.mickare.xserver3.netty;

import com.google.protobuf.ByteString;

import de.mickare.xserver.protocol.Auth.AuthChallenge;
import de.mickare.xserver.protocol.Auth.AuthResponse;
import de.mickare.xserver.protocol.Auth.AuthSuccess;
import de.mickare.xserver.protocol.Error.ErrorMessage;
import de.mickare.xserver.protocol.Network;
import de.mickare.xserver.protocol.Network.NetworkInformation;
import de.mickare.xserver.protocol.Transport.Packet;
import de.mickare.xserver3.exception.ProtocolException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public abstract class AuthHandler extends SimpleChannelInboundHandler<Packet> {

  public static AuthHandler createMaster(Server server) {
    return null;
  }

  public static AuthHandler createSlave() {
    return null;
  }

  public static enum State {
    NEW, PROGRESS, SUCCESS, FAILED;
  }

  @Getter
  protected volatile State state = State.NEW;

  // **********************************************************************************
  // Master Handler to

  @RequiredArgsConstructor
  public static class MasterAuthHandler extends AuthHandler implements ChannelHandler {



    private final Server master;
    private Network.Server slave = null;

    private ByteString challenge = ByteString.EMPTY;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
      switch (msg.getValueCase()) {
        case ERROR:
          handle(ctx, msg.getError());
          break;
        case AUTHRESPONSE:
          handle(ctx, msg.getAuthResponse());
          break;
        default:
          throw new ProtocolException();
      }
    }

    private void handle(ChannelHandlerContext ctx, ErrorMessage error) throws Exception {
      this.state = State.FAILED;
      try {
        PipeUtils.getLogger(ctx)
            .info("Client failed to connect!" + error.getType().name() + " " + error.getText());
      } finally {
        ctx.close();
      }
    }

    private void handle(ChannelHandlerContext ctx, AuthResponse msg) throws Exception {
      if (!this.master.isAuthorized(slave, challenge, msg.getToken())) {
        this.state = State.FAILED;
        try {
          ErrorMessage err = ErrorMessage.newBuilder().setType(ErrorMessage.Type.BAD_CREDENTIALS)
              .setText("").build();
          PipeUtils.getLogger(ctx)
              .info("Client failed to connect!" + err.getType().name() + " " + err.getText());
          ctx.writeAndFlush(Packet.newBuilder().setError(err).build());
        } finally {
          ctx.close();
        }
        return;
      }

      this.state = State.SUCCESS;
      AuthSuccess.ActionCase action = this.master.authorizedAction(slave);
      switch (action) {
        case UPGRADE:
          ctx.writeAndFlush(Packet.newBuilder()
              .setAuthSuccess(AuthSuccess.newBuilder().setUpgrade(true)).build());
          upgrade(ctx);
          break;
        case CONNECTOTHER:
          ctx.writeAndFlush(Packet.newBuilder().setAuthSuccess(AuthSuccess.newBuilder()
              .setConnectOther(this.master.getNetworkManager().getNetworkInfo())).build());
          ctx.close();
          break;
        default:
          throw new ProtocolException("Unknown Auth Success Action \"" + action + "\"");
      }

    }

    private void upgrade(ChannelHandlerContext ctx) {
      NettyConnection con = new NettyConnection(true, this.master.getInfo(), this.slave, ctx);
      ctx.attr(PipeUtils.CONNECTION).set(con);
      ctx.pipeline().replace("auth", "main", con.getMainHandler());
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      ctx.writeAndFlush(Packet.newBuilder().setAuthChallenge(
          AuthChallenge.newBuilder().setToken(this.challenge).setMaster(this.master.getInfo())));
      this.state = State.PROGRESS;
    }

  }

  @RequiredArgsConstructor
  public static class SlaveAuthHandler extends AuthHandler {

    private Network.Server master = null;
    private final Client slave;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
      switch (msg.getValueCase()) {
        case ERROR:
          handle(ctx, msg.getError());
          break;
        case AUTHCHALLENGE:
          handle(ctx, msg.getAuthChallenge());
          break;
        case AUTHSUCCESS:
          handle(ctx, msg.getAuthSuccess());
          break;
        default:
          throw new ProtocolException();
      }
    }

    private void handle(ChannelHandlerContext ctx, ErrorMessage error) throws Exception {
      this.state = State.FAILED;
      try {
        PipeUtils.getLogger(ctx)
            .info("Failed to connect to server!" + error.getType().name() + " " + error.getText());
      } finally {
        ctx.close();
      }
    }

    private void handle(ChannelHandlerContext ctx, AuthChallenge msg) throws Exception {
      this.state = State.PROGRESS;
      this.master = msg.getMaster();
      ByteString responseToken = slave.getResponseToken(this.master, msg.getToken());
      ctx.writeAndFlush(Packet.newBuilder().setAuthResponse(
          AuthResponse.newBuilder().setSlave(this.slave.getInfo()).setToken(responseToken)));
    }

    private void handle(ChannelHandlerContext ctx, AuthSuccess msg) throws Exception {
      switch (msg.getActionCase()) {
        case UPGRADE:
          upgrade(ctx);
          break;
        case CONNECTOTHER:
          NetworkInformation info = msg.getConnectOther();
          this.slave.getNetworkManager().connectOther(master, info);
          break;
        default:
          throw new ProtocolException("Unknown Success Action \"" + msg.getActionCase() + "\"!");
      }
    }


    private void upgrade(ChannelHandlerContext ctx) {
      NettyConnection con = new NettyConnection(false, this.master, this.slave.getInfo(), ctx);
      ctx.attr(PipeUtils.CONNECTION).set(con);
      ctx.pipeline().replace("auth", "main", con.getMainHandler());
    }
  }

}
