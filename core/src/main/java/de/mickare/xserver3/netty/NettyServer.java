package de.mickare.xserver3.netty;

import java.util.logging.Logger;

import com.google.common.util.concurrent.AbstractIdleService;

import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver.protocol.TransportProto;
import de.mickare.xserver3.NetworkManager;
import de.mickare.xserver3.Server;
import de.mickare.xserver3.netty.handshake.HandshakeHandler;
import de.mickare.xserver3.netty.handshake.ServerHandshakeHandler;
import de.mickare.xserver3.security.Security;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;

public class NettyServer extends AbstractIdleService implements Server {

  @Getter
  private final NetworkManager networkManager;

  private Channel channel;

  private EventLoopGroup serverGroup;

  private final Logger getLogger() {
    return networkManager.getLogger();
  }

  public NettyServer(NetworkManager networkManager) {
    this.networkManager = networkManager;
  }

  public int getServerId() {
    return this.networkManager.getServerId();
  }

  public int getSession() {
    return this.networkManager.getSession();
  }

  public String getName() {
    return this.getNetworkManager().getName().orNull();
  }

  @Override
  protected void startUp() throws Exception {

    serverGroup = this.networkManager.createServerGroup();

    ServerBootstrap b = new ServerBootstrap();
    b//
        .group(serverGroup, networkManager.getWorkerGroup())//
        .channel(NioServerSocketChannel.class).option(ChannelOption.SO_TIMEOUT, 1000)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            // Attributes & config
            ch.attr(PipeUtils.LOGGER).set(getLogger());
            ch.config().setAllocator(PooledByteBufAllocator.DEFAULT);

            // Pipeline
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(getSecurity().getSSLContextForServer().newHandler(ch.alloc()));

            pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
            pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));

            pipeline.addLast("protoDecoder",
                new ProtobufDecoder(TransportProto.Packet.getDefaultInstance()));
            pipeline.addLast("protoEncoder", new ProtobufEncoder());

            pipeline.addLast("idleStateHandler", new IdleStateHandler(60, 30, 0));
            pipeline.addLast(new IdleHandler());

            pipeline.addLast(HandshakeHandler.getName(),
                new ServerHandshakeHandler(ch, NettyServer.this));
            pipeline.addLast("exception", new ExceptionHandler(getLogger()));
          }
        });

    this.channel = b.bind(this.networkManager.getPortOrRandom()).sync().channel();

  }

  @Override
  protected void shutDown() throws Exception {
    ChannelFuture f = this.channel.close();
    this.serverGroup.shutdownGracefully();
    f.sync();
  }


  public NetworkProto.Server toProtocol() {
    return this.networkManager.getProtocolServer();
  }

  @Override
  public Security getSecurity() {
    return this.networkManager.getSecurity();
  }

}
