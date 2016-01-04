package de.mickare.xserver3.network;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.mickare.xserver.protocol.NetworkProto.Server;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BalancedTopology implements Topology {

  @NonNull
  private final Network network;

  private static int countMasterEdges(final Node node) {
    return (int) node.getEdges().values().stream().filter(e -> e.getTo() == node).count();
  }


  @Override
  public Optional<Node> nextConnectTarget() {

    Map<Integer, Node> nodes = network.getNodes();

    // Find best target.
    // 1. Prio, Highest Level
    int currentCountCon = Integer.MAX_VALUE; // 2. Prio, Smallest number of connected nodes
    Node current = null;
    for (Node node : nodes.values()) {

      // Skip all nodes that are directly-connected and not connected at all.
      if (node.getLevel() <= 1 || !node.isConnected() || node.isDestroyed()) {
        continue;
      }

      int countCon = countMasterEdges(node);
      if (node.getMaxConnections() > 0 && countCon >= node.getMaxConnections() / 2) {
        continue;
      }

      // Initialize and take first value to compare.
      if (current == null) {
        currentCountCon = countCon;
        current = node;
        continue;
      }

      int nodeLvl = node.getLevel();
      int curLvl = current.getLevel();
      if (nodeLvl > curLvl) {
        currentCountCon = countCon;
        current = node;
      } else if (nodeLvl == curLvl && countCon < currentCountCon) {
        currentCountCon = countCon;
        current = node;
      }
    }

    return Optional.ofNullable(current);
  }

  @Override
  public Optional<Node> nextDisconnectTarget() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean canClientConnect(Server client, boolean forced) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<Server> adviceConnectForward(Server client) {
    // TODO Auto-generated method stub
    return null;
  }

}
