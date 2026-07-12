package com.cryptroot.core.dialogue;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable, validated directed graph of {@link DialogueNode}s with a single entry point. Build
 * instances with {@link #builder()}.
 *
 * <p>The {@link Builder} verifies referential integrity at {@link Builder#build()} time: every
 * {@code nextId} / option target must resolve to a real node, and the start node must exist. This
 * turns authoring mistakes into immediate, descriptive failures rather than runtime surprises.
 */
public final class DialogueGraph {

  private final Map<String, DialogueNode> nodes; // insertion-ordered
  private final String startId;

  private DialogueGraph(Map<String, DialogueNode> nodes, String startId) {
    this.nodes = nodes;
    this.startId = startId;
  }

  /** Id of the entry node. */
  public String startId() {
    return startId;
  }

  /** The entry node. */
  public DialogueNode start() {
    return nodes.get(startId);
  }

  /** Returns the node with {@code id}, or {@code null} if absent. */
  public DialogueNode node(String id) {
    return nodes.get(id);
  }

  /** All nodes in insertion order. */
  public Collection<DialogueNode> nodes() {
    return nodes.values();
  }

  // -------------------------------------------------------------------------
  // Builder
  // -------------------------------------------------------------------------

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link DialogueGraph}. The first node added becomes the start node unless
   * overridden with {@link #start(String)}.
   */
  public static final class Builder {

    private final Map<String, DialogueNode> nodes = new LinkedHashMap<>();
    private String startId;

    /** Adds an arbitrary node. */
    public Builder add(DialogueNode node) {
      Objects.requireNonNull(node, "node must not be null");
      if (nodes.containsKey(node.id())) {
        throw new IllegalArgumentException("Duplicate node id: " + node.id());
      }
      nodes.put(node.id(), node);
      if (startId == null) startId = node.id();
      return this;
    }

    /** Adds a {@link DialogueNode.Line}. */
    public Builder line(String id, Speaker speaker, String text, String nextId) {
      return add(new DialogueNode.Line(id, speaker, text, nextId));
    }

    /** Adds a {@link DialogueNode.Choice}. */
    public Builder choice(String id, Speaker speaker, String prompt, List<Option> options) {
      return add(new DialogueNode.Choice(id, speaker, prompt, options));
    }

    /** Adds a {@link DialogueNode.Action}. */
    public Builder action(
        String id,
        String requirementKey,
        String hintText,
        WaitMode waitMode,
        boolean costsAction,
        String nextId) {
      return add(
          new DialogueNode.Action(id, requirementKey, hintText, waitMode, costsAction, nextId));
    }

    /** Adds a {@link DialogueNode.Event}. */
    public Builder event(String id, DialogueEvent event, String nextId) {
      return add(new DialogueNode.Event(id, event, nextId));
    }

    /** Adds a terminal {@link DialogueNode.End}. */
    public Builder end(String id) {
      return add(new DialogueNode.End(id));
    }

    /** Overrides the entry node (defaults to the first node added). */
    public Builder start(String id) {
      this.startId = id;
      return this;
    }

    /**
     * Validates all references and builds the graph.
     *
     * @throws IllegalStateException if the graph is empty, the start node is missing, or any
     *     successor reference is dangling
     */
    public DialogueGraph build() {
      if (nodes.isEmpty()) {
        throw new IllegalStateException("DialogueGraph has no nodes");
      }
      if (startId == null || !nodes.containsKey(startId)) {
        throw new IllegalStateException("Start node not found: " + startId);
      }
      for (DialogueNode node : nodes.values()) {
        switch (node) {
          case DialogueNode.Line l -> requireNode(node, l.nextId());
          case DialogueNode.Action a -> requireNode(node, a.nextId());
          case DialogueNode.Event e -> requireNode(node, e.nextId());
          case DialogueNode.Choice c -> {
            if (c.options().isEmpty()) {
              throw new IllegalStateException("Choice node '" + c.id() + "' has no options");
            }
            for (Option o : c.options()) requireNode(node, o.nextId());
          }
          case DialogueNode.End ignored -> {
            /* terminal */
          }
        }
      }
      return new DialogueGraph(new LinkedHashMap<>(nodes), startId);
    }

    private void requireNode(DialogueNode from, String targetId) {
      if (targetId == null || !nodes.containsKey(targetId)) {
        throw new IllegalStateException(
            "Node '" + from.id() + "' references missing node: " + targetId);
      }
    }
  }
}
