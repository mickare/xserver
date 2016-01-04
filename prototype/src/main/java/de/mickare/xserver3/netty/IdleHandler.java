package de.mickare.xserver3.netty;

import de.mickare.xserver.protocol.NetworkProto;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class IdleHandler extends ChannelDuplexHandler {

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent e = (IdleStateEvent) evt;
      if (e.state() == IdleState.READER_IDLE) {
        ctx.close();
      } else if (e.state() == IdleState.WRITER_IDLE) {
        PacketUtil.writeAndFlush(ctx, NetworkProto.Heartbeat.newBuilder());
      }
    }
  }

}
