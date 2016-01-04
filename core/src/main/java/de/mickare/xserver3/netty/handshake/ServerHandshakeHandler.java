package de.mickare.xserver3.netty.handshake;

import java.util.List;
import java.util.Random;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.mickare.xserver.protocol.ErrorProto.ErrorMessage;
import de.mickare.xserver.protocol.HandshakeProto.*;
import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver3.exception.ConnectionException;
import de.mickare.xserver3.exception.ProtocolException;
import de.mickare.xserver3.netty.MainHandler;
import de.mickare.xserver3.netty.NettyConnection;
import de.mickare.xserver3.netty.PacketUtil;
import de.mickare.xserver3.netty.PipeUtils;
import de.mickare.xserver3.security.Security;
import de.mickare.xserver3.netty.NettyServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

public class ServerHandshakeHandler extends HandshakeHandler {

  private final ByteString challenge;

  @Getter
  private final NettyServer server;
  @Getter
  private NetworkProto.Server client = null;

  private long challengeTimestamp;
  private int rating = 0; // Higher rating is better.


  public ServerHandshakeHandler(Channel channel, NettyServer server) {
    super(channel);
    Preconditions.checkNotNull(server);
    this.server = server;
    this.challenge = getSecurity().generateChallenge(server);
  }

  private Security getSecurity() {
    return server.getNetworkManager().getSecurity();
  }

  private boolean check(ByteString response) {
    Preconditions.checkNotNull(response);
    Preconditions.checkState(client != null);
    return getSecurity().check(this.server, this.client, this.challenge, response);
  }

  private int generateFreeServerId() {
    return new Random().nextInt();
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, ErrorMessage error) throws Exception {
    try {
      if (this.state() != State.FAILED && this.state() != State.SUCCESS) {
        this.fail(new ConnectionException(error.getType(), true, error.getText()));
      }
      logger(ctx)
          .info("Client failed to connect!" + error.getType().name() + " " + error.getText());
    } finally {
      ctx.close();
    }
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, Login login) throws Exception {
    checkState(State.NEW);

    if (login.getSession() == 0) {
      throw new ConnectionException(ErrorMessage.Type.PROTOCOL_ERROR, true, "Missing session!");
    }

    NetworkProto.Server.Builder cb = NetworkProto.Server.newBuilder();
    cb.setId(login.getServerId());
    cb.setSession(login.getSession());
    cb.setName(login.getName());
    cb.setProtocolVersion(login.getProtocolVersion());
    this.client = cb.buildPartial();

    PacketUtil.writeAndFlush(ctx, AuthChallenge.newBuilder().setToken(challenge));
    this.challengeTimestamp = System.currentTimeMillis();
    this.setState(State.AUTH);

  }

  @Override
  protected void handle(ChannelHandlerContext ctx, AuthChallenge authChallenge) throws Exception {
    throw new ProtocolException("Invalid or unknown packet!");
  }

  @Override
  protected void handle(ChannelHandlerContext ctx, AuthResponse authResponse) throws Exception {
    checkState(State.AUTH);
    if (authResponse.getToken() == null) {
      throw new ConnectionException(ErrorMessage.Type.BAD_CREDENTIALS, true, "Missing token!")
          .doLog();
    }

    if (!check(authResponse.getToken())) {
      throw new ConnectionException(ErrorMessage.Type.UNAUTHORIZED, true, "Wrong credentials!")
          .doLog();
    }

    // Set rating
    long timeDelta = (System.currentTimeMillis() - this.challengeTimestamp);
    timeDelta = timeDelta < 1 ? 1 : timeDelta;
    this.rating = this.challenge.size() + authResponse.getToken().size() / (int) timeDelta;

    AuthSuccess.Builder b = AuthSuccess.newBuilder();
    if (this.client.getId() == 0) {
      NetworkProto.Server.Builder cb = NetworkProto.Server.newBuilder(this.client);
      int id = generateFreeServerId();
      cb.setId(id);
      b.setGeneratedClientId(id);
      this.client = cb.buildPartial();
    }

    PacketUtil.writeAndFlush(ctx, b);
    this.setState(State.ACTION);

  }

  @Override
  protected void handle(ChannelHandlerContext ctx, AuthSuccess authSuccess) throws Exception {
    throw new ProtocolException("Invalid or unknown packet!");
  }


  // @Override
  // protected void handle(ChannelHandlerContext ctx, Introduction intro) throws Exception {
  // checkState(State.INTRO);
  //
  // final NetworkProto.Server old = this.client;
  // this.client = intro.getServer();
  // if (client.getId() == 0 || old.getId() != client.getId()
  // || old.getSession() != client.getSession() || client.getSession() == 0) {
  // throw new ProtocolException("Client ID or session changed!");
  // }
  // if (old.getSettings().getProtocolVersion() != this.client.getSettings().getProtocolVersion()) {
  // throw new ProtocolException("Protocol version changed!");
  // }
  //
  // this.setState(State.ACTION);
  // }


  @Override
  protected void handle(ChannelHandlerContext ctx, ActionRequest actionRequest) throws Exception {
    checkState(State.ACTION);

    switch (actionRequest.getActionCase()) {
      case INFO:
        handleActionInfo(ctx, actionRequest.getInfo());
        break;
      case CONNECT:
        handleActionConnect(ctx, actionRequest.getConnect());
        break;
      default:
        throw new ProtocolException("Unknown action request!");
    }

  }

  private void handleActionConnect(ChannelHandlerContext ctx, ActionRequest.Connect msg)
      throws Exception {

    if (this.server.getNetworkManager().getNetwork().getTopology().canClientConnect(this.client,
        msg.getForced()) //
        && this.server.getNetworkManager().getSecurity().canClientConnect(this.client)) {
      ActionResponse.Builder b = ActionResponse.newBuilder();
      ActionResponse.Connect.Builder c = ActionResponse.Connect.newBuilder();

      NetworkProto.Bridge.Builder bridgeBuilder = NetworkProto.Bridge.newBuilder();
      bridgeBuilder.setMaster(this.server.toProtocol());
      bridgeBuilder.setSlave(this.client);
      bridgeBuilder.setRating(this.rating);
      NetworkProto.Bridge bridge = bridgeBuilder.build();
      c.setUpgrade(bridge);
      b.setConnect(c);
      PacketUtil.writeAndFlush(ctx, b);
      upgradeToBridge(ctx);
      this.setState(State.SUCCESS);
      return;
    }

    if (this.server.getNetworkManager().getSecurity().canClientForward(this.client)) {
      final List<NetworkProto.Server> targets = this.server.getNetworkManager().getNetwork()
          .getTopology().adviceConnectForward(this.client);
      if (!targets.isEmpty()) {
        ActionResponse.Builder b = ActionResponse.newBuilder();
        ActionResponse.Connect.Forward.Builder f = ActionResponse.Connect.Forward.newBuilder();
        f.setServer(this.server.toProtocol());
        f.addAllTargets(targets);
        b.setConnect(ActionResponse.Connect.newBuilder().setForward(f));
        PacketUtil.writeAndFlush(ctx, b);
        this.setState(State.SUCCESS);
        ctx.close();
        return;
      }
    }

    final ActionResponse.Builder b = ActionResponse.newBuilder();
    b.setDeny(true);
    PacketUtil.writeAndFlush(ctx, b);
    this.fail(null);
    ctx.close();
  }

  private void handleActionInfo(ChannelHandlerContext ctx, ActionRequest.Info msg)
      throws Exception {
    ActionResponse.Builder b = ActionResponse.newBuilder();
    ActionResponse.Info.Builder i = ActionResponse.Info.newBuilder();
    i.setInfo(this.server.getNetworkManager().getNetwork().toInfo());
    i.setServer(this.server.toProtocol());
    b.setInfo(i);
    PacketUtil.writeAndFlush(ctx, b);
    upgradeToInfo(ctx);
    this.setState(State.SUCCESS);
  }


  private void upgradeToBridge(ChannelHandlerContext ctx) {
    NettyConnection con = this.server.getNetworkManager().connected(this);
    ctx.attr(PipeUtils.CONNECTION).set(con);
    ctx.pipeline().replace(this, MainHandler.getName(), con.getMainHandler());

  }

  private void upgradeToInfo(ChannelHandlerContext ctx) {

  }


  @Override
  protected void handle(ChannelHandlerContext ctx, ActionResponse actionResponse) throws Exception {
    throw new ProtocolException("Invalid or unknown packet!");
  }

  @Override
  public NetworkProto.Server getProtocolServer() {
    return this.server.toProtocol();
  }

  @Override
  public NetworkProto.Server getProtocolClient() {
    return this.client;
  }

}
