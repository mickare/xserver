package de.mickare.xserver3.security;

import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import com.google.protobuf.ByteString;

import de.mickare.xserver.protocol.NetworkProto.Server;
import de.mickare.xserver3.netty.NettyClient;
import de.mickare.xserver3.netty.NettyServer;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.Getter;

public class NoSecurity implements Security {

  private final SecureRandom random = new SecureRandom();
  private final int challengeLength = 64; // bytes

  @Getter
  private final SslContext SSLContextForClient, SSLContextForServer;

  public NoSecurity() throws SSLException, CertificateException {

    SSLContextForClient = SslContextBuilder.forClient().build();
    SelfSignedCertificate cert = new SelfSignedCertificate();
    SSLContextForServer = SslContextBuilder.forServer(cert.key(), cert.cert()).build();

    random.setSeed(random.generateSeed(challengeLength)); // self setSeed the secure randomizer

  }

  @Override
  public synchronized ByteString generateChallenge(NettyServer connection) {
    final byte[] buf = new byte[challengeLength];
    random.nextBytes(buf);
    return ByteString.copyFrom(buf);
  }

  @Override
  public ByteString generateResponse(NettyClient client, ByteString challenge) {
    return challenge;
  }

  @Override
  public boolean check(NettyServer server, Server client, ByteString challenge,
      ByteString response) {
    return challenge.equals(response);
  }

  @Override
  public boolean canClientConnect(Server client) {
    return true;
  }

  @Override
  public boolean canClientForward(Server client) {
    return true;
  }

}
