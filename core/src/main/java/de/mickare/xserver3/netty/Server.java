package de.mickare.xserver3.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.protobuf.ByteString;

import de.mickare.xserver.protocol.Auth.AuthSuccess.ActionCase;
import de.mickare.xserver.protocol.Network;
import de.mickare.xserver.protocol.Transport;
import de.mickare.xserver3.NetworkManager;

@RequiredArgsConstructor
public class Server extends AbstractIdleService {

  @Data
  public static class Config {
    private final int inetPort;
    private final SslContext sslCtx;
    private final Network.Server info;
  }

  private final Logger logger;

  @Getter
  @NonNull
  private final NetworkManager networkManager;

  private final Config config;


  private EventLoopGroup eventGroup;
  private Channel channel;


  @Override
  protected void startUp() throws Exception {

    this.eventGroup = new NioEventLoopGroup();

    ServerBootstrap b = new ServerBootstrap();
    b.group(eventGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_TIMEOUT, 1000)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            // Attributes & config
            ch.attr(PipeUtils.LOGGER).set(logger);
            ch.config().setAllocator(PooledByteBufAllocator.DEFAULT);

            // Pipeline
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(config.sslCtx.newHandler(ch.alloc()));
            pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
            pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
            pipeline.addLast("protoDecoder",
                new ProtobufDecoder(Transport.Packet.getDefaultInstance()));
            pipeline.addLast("protoEncoder", new ProtobufEncoder());
            pipeline.addLast("auth", AuthHandler.createMaster(Server.this));
            pipeline.addLast("exception", new ExceptionHandler(logger));
          }
        });

    this.channel = b.bind(config.inetPort).channel();

  }

  @Override
  protected void shutDown() throws Exception {
    this.channel.close();
    // Shutdown EventLoopGroup and all channels that belong to the group.
    this.eventGroup.shutdownGracefully().await(5, TimeUnit.SECONDS);
  }

  public Network.Server getInfo() {
    return config.info;
  }

  public boolean isAuthorized(de.mickare.xserver.protocol.Network.Server slave, ByteString challenge,
      ByteString token) {
    return false;
  }

  public ActionCase authorizedAction(Network.Server slave) {
    // TODO Auto-generated method stub
    return null;
  }



}
