package de.mickare.xserver3.netty.handshake;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.mickare.xserver.protocol.ErrorProto.ErrorMessage;
import de.mickare.xserver.protocol.HandshakeProto.ActionRequest;
import de.mickare.xserver.protocol.HandshakeProto.ActionResponse;
import de.mickare.xserver.protocol.HandshakeProto.AuthChallenge;
import de.mickare.xserver.protocol.HandshakeProto.AuthResponse;
import de.mickare.xserver.protocol.HandshakeProto.AuthSuccess;
import de.mickare.xserver.protocol.HandshakeProto.Login;
import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver.protocol.NetworkProto.Bridge;
import de.mickare.xserver3.NetworkManager;
import de.mickare.xserver3.exception.ConnectionException;
import de.mickare.xserver3.exception.ProtocolException;
import de.mickare.xserver3.netty.NettyClient;
import de.mickare.xserver3.netty.MainHandler;
import de.mickare.xserver3.netty.NettyConnection;
import de.mickare.xserver3.netty.PacketUtil;
import de.mickare.xserver3.netty.PipeUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class ClientHandshakeHandler extends HandshakeHandler {

  @Getter
  @RequiredArgsConstructor
  public static class Action {
    public Action(ActionRequest.ActionCase actionCase) {
      this(actionCase, false);
    }

    @NonNull
    private final ActionRequest.ActionCase actionCase;
    private final boolean forcedAction;
  }

  @Getter
  private final NettyClient client;

  @Getter
  private final String ip;
  @Getter
  private final int port;
  @Getter
  private NetworkProto.Server server = null;

  @Getter
  private final Action action;


  public ClientHandshakeHandler(Channel channel, NettyClient client, String ip, int port,
      Action action) {
    super(channel);
    Preconditions.checkNotNull(client);
    Preconditions.checkNotNull(ip);
    Preconditions.checkArgument(!ip.isEmpty());
    Preconditions.checkArgument(port > 0);
    Preconditions.checkArgument(client.getSession() != 0);
    Preconditions.checkNotNull(action);
    this.client = client;
    this.action = action;
    this.ip = ip;
    this.port = port;
  }

  private ByteString respond(ByteString challenge) {
    return this.client.getSecurity().generateResponse(client, challenge);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    super.handlerAdded(ctx);

    Login.Builder b = Login.newBuilder();
    b.setProtocolVersion(NetworkManager.PROTOCOL_VERSION);
    b.setServerId(client.getServerId());
    b.setName(client.getName());
    b.setSession(client.getSession());

    PacketUtil.writeAndFlush(ctx, b);

  }

  @Override
  protected void handle(ChannelHandlerContext ctx, ErrorMessage error) throws Exception {
    try {
      if (this.state() != State.FAILED && this.state() != State.SUCCESS) {
        this.fail(new ConnectionException(error.getType(), true, error.getText()));
      }
      PipeUtils.getLogger(ctx)
          .info("Connect to server failed!" + error.getType().name() + " " + error.getText());
    } finally {
      ctx.close();
    }

  }

  @Override
  protected void handle(ChannelHandlerContext ctx, Login login) throws Exception {
    throw new ProtocolException("Invalid or unknown packet!");
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, AuthChallenge authChallenge) throws Exception {
    this.checkState(State.NEW);

    if (authChallenge.getToken() == null) {
      throw new ProtocolException("token is null");
    }

    AuthChallenge.Builder b = AuthChallenge.newBuilder();
    b.setToken(respond(authChallenge.getToken()));

    PacketUtil.writeAndFlush(ctx, b);

    this.setState(State.AUTH);
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, AuthResponse authResponse) throws Exception {
    throw new ProtocolException("Invalid or unknown packet!");
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, AuthSuccess authSuccess) throws Exception {
    this.checkState(State.NEW);

    if (this.client.getServerId() == 0) {
      if (authSuccess.getGeneratedClientId() == 0) {
        throw new ProtocolException("Server did not generate free Server ID!");
      }
      this.client.getNetworkManager().setServerId(authSuccess.getGeneratedClientId());
    }

    final ActionRequest.Builder b = ActionRequest.newBuilder();
    switch (this.action.actionCase) {
      case INFO:
        b.setInfo(ActionRequest.Info.newBuilder());
        break;
      case CONNECT:
        b.setConnect(ActionRequest.Connect.newBuilder().setClient(this.client.toProtocol())
            .setForced(this.action.isForcedAction()));
        break;
      default:
        throw new ProtocolException("Invalid action!");
    }

    PacketUtil.writeAndFlush(ctx, b);

    this.setState(State.ACTION);
  }

  // @Override
  // protected void handle(ChannelHandlerContext ctx, Introduction intro) throws Exception {
  // this.checkState(State.INTRO);
  //
  // this.server = intro.getServer();
  // if (server.getId() == 0 || server.getSession() == 0) {
  // throw new ProtocolException("Server ID or session invalid!");
  // }
  // if (server.getProtocolVersion() == 0 || server.getPort() == 0) {
  // throw new ProtocolException("Invalid introduction packet!");
  // }
  //
  // ActionRequest.Builder b = ActionRequest.newBuilder();
  // b.setAction(this.action);
  // PacketUtil.writeAndFlush(ctx, b);
  //
  // this.setState(State.ACTION);
  // }

  @Override
  protected void handle(ChannelHandlerContext ctx, ActionRequest actionRequest) throws Exception {
    throw new ProtocolException("Invalid or unknown packet!");
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, ActionResponse msg) throws Exception {
    this.checkState(State.ACTION);

    switch (msg.getResponseCase()) {
      case INFO:
        if (this.action.actionCase != ActionRequest.ActionCase.INFO) {
          throw new ProtocolException("Server responded with wrong action!");
        }
        this.handleInfo(ctx, msg.getInfo());
        this.setState(State.SUCCESS);
        break;
      case CONNECT:
        if (this.action.actionCase != ActionRequest.ActionCase.CONNECT) {
          throw new ProtocolException("Server responded with wrong action!");
        }
        handleConnect(ctx, msg.getConnect());
        this.setState(State.SUCCESS);
        break;
      case DENY:
        try {
          this.fail(
              new ConnectionException(ErrorMessage.Type.UNAUTHORIZED, true, "Action denied!"));
        } finally {
          ctx.close();
        }
        break;
      default:
        throw new ProtocolException("Unknown action response!");
    }

  }

  private void handleInfo(ChannelHandlerContext ctx, ActionResponse.Info info) throws Exception {
    this.server = info.getServer();
    info.getInfo();
  }

  private void handleConnect(ChannelHandlerContext ctx, ActionResponse.Connect connect)
      throws Exception {

    switch (connect.getActionCase()) {
      case UPGRADE:
        upgradeToConnect(ctx, connect.getUpgrade());
        this.setState(State.SUCCESS);
        break;
      case FORWARD:
        sendForward(ctx, connect.getForward());
        this.setState(State.SUCCESS);
        ctx.close();
        break;
      default:
        throw new ProtocolException("Unknown connect action!");
    }

  }


  private void sendForward(ChannelHandlerContext ctx, ActionResponse.Connect.Forward forward) {
    final List<NetworkProto.Server> forwards = forward.getTargetsList();
    this.client.getNetworkManager().connectForwardTo(forward.getServer(), forwards);
  }

  private void upgradeToConnect(ChannelHandlerContext ctx, Bridge bridge) {
    NettyConnection con = this.client.getNetworkManager().connected(this);
    ctx.attr(PipeUtils.CONNECTION).set(con);
    ctx.pipeline().replace(this, MainHandler.getName(), con.getMainHandler());
  }

  @Override
  public NetworkProto.Server getProtocolServer() {
    return this.server;
  }

  @Override
  public NetworkProto.Server getProtocolClient() {
    return this.client.toProtocol();
  }


}
