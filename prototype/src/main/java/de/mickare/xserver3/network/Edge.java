package de.mickare.xserver3.network;

import com.google.common.base.Preconditions;

import lombok.Getter;

public class Edge {

  @Getter
  private final Node from, to;

  @Getter
  private final float rating;

  public Edge(Node from, Node to, float rating) {
    Preconditions.checkNotNull(from);
    Preconditions.checkNotNull(to);
    this.from = from;
    this.to = to;
    this.rating = rating;
  }

  /**
   * Gets the other of the given node
   * 
   * @param node given
   * @return other node of the given node.
   * @throws IllegalArgumentException if node is not part of the edge.
   */
  public Node getOther(Node node) throws IllegalArgumentException {
    if (from == node) {
      return to;
    } else if (to == node) {
      return from;
    }
    throw new IllegalArgumentException("Node is not part of this edge!");
  }

  /**
   * Checks if the edge is valid. Both nodes need not to be destroyed to be valid.
   * 
   * @return false if one of the nodes is destroyed
   */
  public final boolean isValid() {
    return !from.isDestroyed() && !to.isDestroyed();
  }
  
}
