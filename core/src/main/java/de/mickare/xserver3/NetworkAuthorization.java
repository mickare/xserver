package de.mickare.xserver3;

import com.google.protobuf.ByteString;

import de.mickare.xserver.protocol.NetworkProto;

public interface NetworkAuthorization {

  boolean isAuthorized(NetworkProto.Server slave, ByteString challenge, ByteString response);

}
