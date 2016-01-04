package de.mickare.xserver.netty;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;

import com.google.protobuf.ByteString;

import de.mickare.xserver.protocol.Network.Server;
import de.mickare.xserver3.security.Security;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class SimpleSecurity implements Security {

  private final SecureRandom random = new SecureRandom();

  private final SelfSignedCertificate ssc;
  private final SslContext sslCtxForClient;
  private final SslContext sslCtxForServer;

  public SimpleSecurity() throws CertificateException, SSLException, NoSuchAlgorithmException {
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("DSA");
    this.ssc = new SelfSignedCertificate();
    sslCtxForClient = SslContextBuilder.forClient()//
        .trustManager(InsecureTrustManagerFactory.INSTANCE)//
        .keyManager(kmf).build();
    sslCtxForServer = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
        // .clientAuth(ClientAuth.REQUIRE)//
        .keyManager(kmf).build();
  }

  @Override
  public ByteString generateChallenge(final NettyServer server) {
    final byte[] bytes = new byte[16];
    random.nextBytes(bytes);
    return ByteString.copyFrom(bytes);
  }

  @Override
  public ByteString generateResponse(NettyServer server, NettyServer client, ByteString challenge) {
    return challenge;
  }

  @Override
  public boolean check(NettyServer server, NettyServer client, ByteString challenge, ByteString response) {
    return challenge.equals(response);
  }

  @Override
  public SslContext getSSLContextForClient() {
    return sslCtxForClient;
  }

  @Override
  public SslContext getSSLContextForServer() {
    return sslCtxForServer;
  }

}
