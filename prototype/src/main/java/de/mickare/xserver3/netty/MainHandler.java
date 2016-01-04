package de.mickare.xserver3.netty;

import de.mickare.xserver.protocol.ErrorProto.ErrorMessage;
import de.mickare.xserver.protocol.NetworkProto.Connection;
import de.mickare.xserver.protocol.NetworkProto.Disconnection;
import de.mickare.xserver.protocol.NetworkProto.NetworkInformation;
import de.mickare.xserver.protocol.TransportProto.Broadcast;
import de.mickare.xserver.protocol.TransportProto.Message;
import de.mickare.xserver.protocol.TransportProto.Packet;
import de.mickare.xserver3.exception.ProtocolException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MainHandler extends SimpleChannelInboundHandler<Packet> {
  @Getter
  private static final String name = "main";

  @NonNull
  private final NettyConnection connection;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
    switch (msg.getValueCase()) {
      case ERROR:
        handle(ctx, msg.getError());
        break;
      case MESSAGE:
        handle(ctx, msg.getMessage());
        break;
      case BROADCAST:
        handle(ctx, msg.getBroadcast());
        break;
      case NETINFO:
        handle(ctx, msg.getNetInfo());
        break;
      case CONNECTION:
        handle(ctx, msg.getConnection());
        break;
      case DISCONNECTION:
        handle(ctx, msg.getDisconnection());
        break;
      default:
        throw new ProtocolException("Invalid or unknown packet!");
    }
  }

  private void handle(ChannelHandlerContext ctx, Disconnection disconnect) {
    // TODO Auto-generated method stub

  }

  private void handle(ChannelHandlerContext ctx, Connection connect) {
    // TODO Auto-generated method stub

  }

  private void handle(ChannelHandlerContext ctx, NetworkInformation netInfo) {
    // TODO Auto-generated method stub

  }

  private void handle(ChannelHandlerContext ctx, Broadcast broadcast) {
    // TODO Auto-generated method stub

  }

  private void handle(ChannelHandlerContext ctx, Message message) {
    // TODO Auto-generated method stub

  }

  private void handle(ChannelHandlerContext ctx, ErrorMessage error) {
    switch (error.getType()) {

      default:
        try {
          PipeUtils.getLogger(ctx)
              .info("Connection Error!" + error.getType().name() + " " + error.getText());
        } finally {
          ctx.close();
        }
    }
  }

}
