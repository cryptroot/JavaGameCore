package com.cryptroot.core.dialogue;

import java.util.Objects;

/**
 * One selectable option within a {@link DialogueNode.Choice}.
 *
 * @param text label shown to the player
 * @param nextId id of the node to route to when selected
 * @param enabledWhen optional gate; {@code null} means always enabled
 * @param onSelect optional side-effect applied to the blackboard on selection (before routing);
 *     {@code null} means none
 */
public record Option(
    String text, String nextId, DialogueCondition enabledWhen, DialogueEffect onSelect) {

  public Option {
    Objects.requireNonNull(text, "text must not be null");
    Objects.requireNonNull(nextId, "nextId must not be null");
  }

  /** An always-enabled option with no side-effect. */
  public static Option to(String text, String nextId) {
    return new Option(text, nextId, null, null);
  }

  /** An always-enabled option that applies {@code effect} when selected. */
  public static Option to(String text, String nextId, DialogueEffect effect) {
    return new Option(text, nextId, null, effect);
  }

  /** Returns {@code true} if this option is enabled against {@code bb}. */
  public boolean isEnabled(DialogueBlackboard bb) {
    return enabledWhen == null || enabledWhen.test(bb);
  }
}
