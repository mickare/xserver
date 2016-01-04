package de.mickare.xserver3.network;

import java.util.List;
import java.util.Optional;

import de.mickare.xserver.protocol.NetworkProto;

public interface Topology {

  Optional<Node> nextConnectTarget();

  Optional<Node> nextDisconnectTarget();

  boolean canClientConnect(NetworkProto.Server client, boolean forced);

  List<NetworkProto.Server> adviceConnectForward(NetworkProto.Server client);

}
