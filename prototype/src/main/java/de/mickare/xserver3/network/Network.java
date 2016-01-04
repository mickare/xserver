package de.mickare.xserver3.network;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import de.mickare.xserver.protocol.NetworkProto;
import de.mickare.xserver.protocol.NetworkProto.NetworkInformation;
import de.mickare.xserver.protocol.NetworkProto.Server;
import de.mickare.xserver3.NetworkManager;
import lombok.Getter;

public class Network {

  private final ConcurrentMap<Integer, Node> nodes = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Node> nodesByName = new MapMaker().weakValues().makeMap();

  @Getter
  private final NetworkManager manager;

  @Getter
  private final Topology topology;

  public Network(NetworkManager manager, Topology topology) {
    Preconditions.checkNotNull(manager);
    Preconditions.checkNotNull(topology);
    this.manager = manager;
    this.topology = topology;



    Futures.addCallback(manager.getServerIdFuture(), new FutureCallback<Integer>() {
      @Override
      public void onFailure(Throwable t) {}

      @Override
      public void onSuccess(Integer id) {
        Node root = getNode(id);
        if (root != null) {
          root.setRoot(true);
        }
      }
    });
  }

  public int getRootId() {
    return manager.getServerId();
  }

  private synchronized Node computeNodeIfAbsent(NetworkProto.Server server) {
    Preconditions.checkArgument(server.getId() != 0);
    Preconditions.checkArgument(server.getSession() != 0);
    Node node = this.nodes.computeIfAbsent(server.getId(), (k) -> new Node(server));
    if (node.getSession() != server.getSession() || node.isDestroyed()) {
      node.destroy();
      node = new Node(server);
      nodes.put(node.getId(), node);
    }

    if (server.getName() != null) {
      nodesByName.put(server.getName().toLowerCase(), node);
    } else if (node.getName() != null) {
      nodesByName.remove(node.getName(), node);
    }
    node.setName(server.getName());

    if (node.getId() == getRootId() && !node.isRoot()) {
      node.setRoot(true);
    }
    return node;
  }

  public Map<Integer, Node> getNodes() {
    return Collections.unmodifiableMap(this.nodes);
  }

  public Map<String, Node> getNodesByName() {
    return Collections.unmodifiableMap(this.nodesByName);
  }

  /**
   * Gets the node with the given id.
   * 
   * @param id of node
   * @return null if not found or node is destroyed.
   */
  public final Node getNode(final int id) {
    final Node node = this.nodes.get(id);
    if (node != null && node.isDestroyed()) {
      this.nodes.remove(id, node);
      return null;
    }
    return node;
  }

  /**
   * Gets the node with the id and session.
   * 
   * @param id of the node
   * @param session of the node
   * @return null if not found or node is destroyed.
   */
  public Node getNode(int id, int session) {
    final Node node = this.getNode(id);
    if (node != null && node.getSession() == session) {
      return node;
    }
    return null;
  }

  /**
   * 
   * @param server
   * @return
   */
  public Node getNode(Server server) {
    return getNode(server.getId(), server.getSession());
  }

  public Node getNode(String name) {
    final Node node = this.nodesByName.get(name.toLowerCase());
    if (node.isDestroyed()) {
      this.nodesByName.remove(name.toLowerCase(), node);
      return getNode(name);
    }
    return node;
  }

  /**
   * Cleans the network from all destroyed nodes.
   */
  public void clean() {
    this.nodes.values().stream().filter(Node::isDestroyed).forEach(n -> nodes.remove(n.getId(), n));
  }

  /**
   * Adds a edge to the network. This method can dynamically create new nodes and change the
   * networks appearance.
   * 
   * @param bridge between two nodes
   * @return edge that is created
   */
  public Edge addEdge(NetworkProto.Bridge bridge) {
    Node master = computeNodeIfAbsent(bridge.getMaster());
    Node slave = computeNodeIfAbsent(bridge.getSlave());
    return slave.connect(master, bridge.getRating());
  }

  /**
   * Removes a edge from the network.
   * 
   * @param bridge between two nodes
   * @return edge that is removed
   */
  public Edge removeEdge(NetworkProto.Bridge bridge) {
    Node master = getNode(bridge.getMaster());
    Node slave = getNode(bridge.getSlave());

    Edge old;
    if (slave == null) {
      old = master.disconnect(bridge.getSlave().getId(), bridge.getSlave().getSession());
    } else if (master == null) {
      old = slave.disconnect(bridge.getMaster().getId(), bridge.getMaster().getSession());
    } else {
      old = master.disconnect(slave);
    }

    clean();
    return old;
  }

  public NetworkInformation toInfo() {
    return null;
  }

}
