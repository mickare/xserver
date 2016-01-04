package de.mickare.xserver3;

import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver3.security.Security;

public interface Member {

  NetworkManager getNetworkManager();

  Security getSecurity();

  int getServerId();

  int getSession();

  String getName();

  NetworkProto.Server toProtocol();

}
