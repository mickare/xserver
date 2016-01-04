package de.mickare.xserver3.network;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.mickare.xserver.protocol.NetworkProto;
import lombok.Getter;
import lombok.Setter;

public class Node {

  private static final void notZero(final int value) throws IllegalArgumentException {
    if (value == 0) {
      throw new IllegalArgumentException("zero");
    }
  }

  @Getter
  private final int id;
  @Getter
  private final int session;
  @Getter
  @Setter
  private String name;
  @Getter
  private final String ip;
  @Getter
  private final int port;
  @Getter
  private final int protocolVersion;
  @Getter
  private final int maxConnections;

  private final Map<Integer, Edge> edges = Maps.newConcurrentMap();
  private final Map<Integer, Edge> edgesUnmod = Collections.unmodifiableMap(edges);

  @Getter
  private volatile boolean destroyed = false;

  private volatile boolean dirty = true;
  private int level = Integer.MAX_VALUE;

  @Getter
  private boolean root = false;

  public Node(final NetworkProto.Server server) {
    notZero(server.getId());
    notZero(server.getSession());
    notZero(server.getPort());
    notZero(server.getProtocolVersion());
    notZero(server.getMaxConnections());

    this.id = server.getId();
    this.session = server.getSession();
    this.name = server.getName();
    this.ip = server.getIp();
    this.port = server.getPort();
    this.protocolVersion = server.getProtocolVersion();
    this.maxConnections = server.getMaxConnections();
  }

  public NetworkProto.Server toProtocol() {
    NetworkProto.Server.Builder b = NetworkProto.Server.newBuilder();
    b.setId(id);
    b.setSession(session);
    b.setName(name);
    b.setIp(ip);
    b.setPort(port);
    b.setProtocolVersion(protocolVersion);
    b.setMaxConnections(maxConnections);
    return b.build();
  }

  private void dirty() {
    this.dirty = true;
  }

  /**
   * Connects this node to the other node.
   * 
   * @param other node
   * @param rating of the connection
   * @return newly created edge
   */
  public Edge connect(final Node other, final float rating) {
    Preconditions.checkArgument(!this.destroyed);
    Preconditions.checkArgument(!other.destroyed);
    Preconditions.checkArgument(other.id != this.id);
    final Edge edge = new Edge(this, other, rating);
    this.addEdge(edge);
    other.addEdge(edge);
    return edge;
  }

  /**
   * Disconnects this node from another node and removes the edge.
   * 
   * @param other node
   * @return edge that connected the other node, or null if not connected to the other node
   */
  public Edge disconnect(final Node other) {
    Preconditions.checkArgument(other.id != this.id);
    final Edge edge = this.edges.get(other.id);
    if (edge != null && edge.getOther(this).session == other.session) {
      other.removeEdge(edge);
      this.removeEdge(edge);
      return edge;
    }
    return null;
  }

  /**
   * Disconnects this node from another node and removes the edge.
   * 
   * @param otherId
   * @param otherSession
   * @return edge that connected the other node, or null if not connected to the other node
   */
  public Edge disconnect(int otherId, int otherSession) {
    Preconditions.checkArgument(this.id != otherId);
    final Edge edge = this.edges.get(otherId);
    if (edge != null) {
      Node other = edge.getOther(this);
      if (other.session == otherSession) {
        other.removeEdge(edge);
        this.removeEdge(edge);
        return edge;
      }
    }
    return null;
  }

  private Edge addEdge(final Edge edge) {
    if (edge.isValid()) {
      return null;
    }
    final Edge old = this.edges.put(edge.getOther(this).getId(), edge);
    dirty = true;
    return old;
  }

  private boolean removeEdge(final Edge edge) {
    boolean result = this.edges.remove(edge.getOther(this), edge);
    dirty = true;
    if (this.edges.isEmpty()) {
      this.destroy();
    }
    return result;
  }

  /**
   * Gets the edge to another node.
   * 
   * @param otherId is the id of the other node.
   * @return edge, or null if not connected directly.
   */
  public Edge getEdge(final int otherId) {
    return this.edges.get(otherId);
  }

  /**
   * Gets an unmodifalbe view of the node's edges.
   * 
   * @return map of edges
   */
  public Map<Integer, Edge> getEdges() {
    return this.edgesUnmod;
  }

  protected synchronized void destroy() {
    if (!this.destroyed) {
      this.destroyed = true;
      this.dirty = true;
      this.edges.values().stream().forEach(e -> e.getOther(this).removeEdge(e));
      this.edges.clear();
    }
  }

  /**
   * Gets the size of edges/connection the node has.
   * 
   * @return size of edges.
   */
  public int size() {
    return edges.size();
  }

  /**
   * Checks if the node is connected directly or via other nodes to a root node.
   * 
   * @return false if not connected.
   */
  public boolean isConnected() {
    return getLevel() != Integer.MAX_VALUE;
  }

  /**
   * Sets this node to a root node
   * 
   * @param root true if node is root
   */
  protected void setRoot(boolean root) {
    this.root = root;
    this.dirty = true;
    this.edges.values().stream().map(e -> e.getOther(this)).forEach(Node::dirty);
  }

  private final Comparator<Edge> bestEdge = (a, b) -> {
    final Node oa = a.getOther(this);
    final Node ob = b.getOther(this);
    int l = Integer.compare(oa.getLevel(), ob.getLevel());
    if (l != 0)
      return l;
    int f = -Float.compare(a.getRating(), b.getRating());
    if (f != 0)
      return f;
    return Integer.compare(oa.getId(), ob.getId());
  };

  private synchronized void computeLevel() {
    if (this.dirty) {
      if (this.root) {
        this.level = 0;
        this.dirty = false;
      } else {
        // Set level and dirty state before completion to prevent a deadlock in a circle situation.
        this.level = Integer.MAX_VALUE;
        this.dirty = false;

        if (!this.destroyed) {
          Optional<Edge> o = this.edges.values().stream().filter(Edge::isValid).min(bestEdge);
          if (o.isPresent()) {
            int otherLevel = o.get().getOther(this).getLevel();
            this.level = otherLevel < Integer.MAX_VALUE ? otherLevel + 1 : Integer.MAX_VALUE;
          }
        }
      }
    }
  }

  /**
   * Gets the level of the node. A level is the distance from the root node. The level is lazy
   * computated, so this method could take more time than a simple getter.
   * 
   * @return level
   */
  public synchronized int getLevel() {
    computeLevel();
    return this.level;
  }

  private List<Node> createPathToRoot(LinkedList<Node> list) {
    if (getLevel() == Integer.MAX_VALUE || this.destroyed) {
      return Collections.emptyList();
    }
    list.push(this);
    if (getLevel() == 0) {
      return list;
    }
    Optional<Edge> o = this.edges.values().stream().filter(Edge::isValid).min(bestEdge);
    if (o.isPresent()) {
      return o.get().getOther(this).createPathToRoot(list);
    }
    return Collections.emptyList();
  }

  public List<Node> getPathToRoot() {
    return createPathToRoot(Lists.newLinkedList());
  }

  public int getWeight() {
    if (this.destroyed) {
      return 0;
    }
    return this.edges.values().stream().map(e -> e.getOther(this))
        .filter(n -> n.getLevel() > this.getLevel()).mapToInt(Node::getWeight).sum();
  }

  private int getWeight(Set<Node> visited) {
    return this.edges.values().stream().map(e -> e.getOther(this))
        .filter(n -> n.getLevel() > this.getLevel()).mapToInt(Node::getWeight).sum();
  }

}
