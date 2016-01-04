package de.mickare.xserver3.netty;

import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver.protocol.TransportProto;
import de.mickare.xserver3.Client;
import de.mickare.xserver3.NetworkManager;
import de.mickare.xserver3.netty.handshake.ClientHandshakeHandler;
import de.mickare.xserver3.netty.handshake.HandshakeHandler;
import de.mickare.xserver3.security.Security;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NettyClient implements Client {

  @Getter
  @NonNull
  private final NetworkManager networkManager;

  public ChannelFuture connect(final String ip, final int port,
      final ClientHandshakeHandler.Action action) throws InterruptedException {
    Preconditions.checkArgument(!ip.isEmpty());
    Preconditions.checkArgument(port > 0);

    Bootstrap b = new Bootstrap();
    b.group(networkManager.getWorkerGroup());
    b.channel(NioSocketChannel.class);
    b.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel ch) throws Exception {
        // Attributes & config
        ch.attr(PipeUtils.LOGGER).set(getLogger());
        ch.config().setAllocator(PooledByteBufAllocator.DEFAULT);

        // Pipeline
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(getSecurity().getSSLContextForClient().newHandler(ch.alloc()));
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        pipeline.addLast("protoDecoder",
            new ProtobufDecoder(TransportProto.Packet.getDefaultInstance()));
        pipeline.addLast("protoEncoder", new ProtobufEncoder());
        pipeline.addLast(HandshakeHandler.getName(),
            new ClientHandshakeHandler(ch, NettyClient.this, ip, port, action));
        pipeline.addLast("exception", new ExceptionHandler(getLogger()));
      }
    });

    return b.bind(ip, port).sync();
  }

  @Override
  public Security getSecurity() {
    return this.networkManager.getSecurity();
  }

  private final Logger getLogger() {
    return networkManager.getLogger();
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

  public NetworkProto.Server toProtocol() {
    return this.networkManager.getProtocolServer();
  }

}
