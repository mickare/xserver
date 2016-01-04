package de.mickare.xserver3.security;

import com.google.protobuf.ByteString;

import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver3.netty.NettyClient;
import de.mickare.xserver3.netty.NettyServer;
import io.netty.handler.ssl.SslContext;

public interface Security {

  ByteString generateChallenge(NettyServer connection);

  ByteString generateResponse(NettyClient client, ByteString challenge);

  boolean check(NettyServer server, NetworkProto.Server client, ByteString challenge,
      ByteString response);

  SslContext getSSLContextForClient();

  SslContext getSSLContextForServer();

  boolean canClientConnect(NetworkProto.Server client);

  boolean canClientForward(NetworkProto.Server client);


}
