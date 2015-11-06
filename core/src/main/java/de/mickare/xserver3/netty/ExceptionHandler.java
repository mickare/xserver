package de.mickare.xserver3.netty;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.mickare.xserver.protocol.Error.ErrorMessage;
import de.mickare.xserver.protocol.Transport.Packet;
import de.mickare.xserver3.exception.ConnectionException;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExceptionHandler extends ChannelHandlerAdapter {

  private final Logger logger;
  
  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    boolean fatal = true;
    try {
      ErrorMessage.Type type;
      String text;
      if (cause instanceof ConnectionException) {
        fatal = ((ConnectionException) cause).isFatal();
        type = ((ConnectionException) cause).type();
        text = cause.getMessage();
      } else {
        type = ErrorMessage.Type.SERVER_ERROR;
        text = "Server Error";
      }
      ErrorMessage.Builder error = ErrorMessage.newBuilder().setType(type).setText(text);
      ctx.writeAndFlush(Packet.newBuilder().setError(error).build())
          .addListener(ChannelFutureListener.CLOSE);
    } catch (final Exception ex) {
      logger.log(Level.SEVERE, "ERROR trying to close socket because we got an unhandled exception",
          ex);
    } finally {
      if (fatal) {
        ctx.close().addListener((v) -> {
          logger.log(Level.INFO, "Connection closed");
        });
      }
    }
  }

}
