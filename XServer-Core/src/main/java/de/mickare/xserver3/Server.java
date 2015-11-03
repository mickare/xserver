package de.mickare.xserver3;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

import com.google.common.util.concurrent.AbstractIdleService;

@RequiredArgsConstructor
public class Server extends AbstractIdleService {

  private final int inetPort;

  private EventLoopGroup eventGroup;
  private Channel channel;

  @Override
  protected void startUp() throws Exception {

    this.eventGroup = new NioEventLoopGroup();

    ServerBootstrap b = new ServerBootstrap();
    b.group(eventGroup).channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_TIMEOUT, 1000)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
            
          }
        });

    this.channel = b.bind(inetPort).channel();

  }

  @Override
  protected void shutDown() throws Exception {
    this.channel.close();
    this.eventGroup.shutdownGracefully();
    this.channel.closeFuture().await(5, TimeUnit.SECONDS);
  }

}
