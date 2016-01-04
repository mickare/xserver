package de.mickare.xserver3;

import de.mickare.xserver3.netty.handshake.ClientHandshakeHandler;
import io.netty.channel.ChannelFuture;

public interface Client extends Member {


  ChannelFuture connect(final String ip, final int port, final ClientHandshakeHandler.Action action)
      throws InterruptedException;

}
