package de.mickare.xserver3.netty;

import com.google.protobuf.ByteString;

import de.mickare.xserver.protocol.Network;
import de.mickare.xserver3.NetworkManager;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Client {

  @Getter
  @NonNull
  private final NetworkManager networkManager;

  public Network.Server getInfo() {
    // TODO Auto-generated method stub
    return null;
  }

  public ByteString getResponseToken(Network.Server master, ByteString token) {
    // TODO Auto-generated method stub
    return null;
  }

}
