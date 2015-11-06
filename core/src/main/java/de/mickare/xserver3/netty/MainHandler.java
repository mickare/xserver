package de.mickare.xserver3.netty;

import de.mickare.xserver.protocol.Error.ErrorMessage;
import de.mickare.xserver.protocol.Network.ConnectionAdd;
import de.mickare.xserver.protocol.Network.ConnectionRemove;
import de.mickare.xserver.protocol.Network.NetworkInformation;
import de.mickare.xserver.protocol.Transport.Broadcast;
import de.mickare.xserver.protocol.Transport.Message;
import de.mickare.xserver.protocol.Transport.Packet;
import de.mickare.xserver3.exception.ProtocolException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MainHandler extends SimpleChannelInboundHandler<Packet> {

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
      case NETCONADD:
        handle(ctx, msg.getNetConAdd());
        break;
      case NETCONREMOVE:
        handle(ctx, msg.getNetConRemove());
        break;
      default:
        throw new ProtocolException();
    }
  }

  private void handle(ChannelHandlerContext ctx, ConnectionRemove netConRemove) {
    // TODO Auto-generated method stub

  }

  private void handle(ChannelHandlerContext ctx, ConnectionAdd netConAdd) {
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
