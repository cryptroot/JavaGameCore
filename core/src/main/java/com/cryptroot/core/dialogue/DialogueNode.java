package com.cryptroot.core.dialogue;

import java.util.List;
import java.util.Objects;

/**
 * A single node in a {@link DialogueGraph}.
 *
 * <p>Every node carries a stable string {@link #id()} (used for traversal and, in future,
 * persistence) and references its successor(s) by id. The node variants map directly onto the
 * conversation flow:
 *
 * <ul>
 *   <li>{@link Line} — a spoken line; advances on click.
 *   <li>{@link Choice} — presents {@link Option}s and branches.
 *   <li>{@link Action} — pauses until a gameplay requirement is met.
 *   <li>{@link Event} — fires a {@link DialogueEvent} side-effect.
 *   <li>{@link End} — terminates the conversation.
 * </ul>
 */
public sealed interface DialogueNode
    permits DialogueNode.Line,
        DialogueNode.Choice,
        DialogueNode.Action,
        DialogueNode.Event,
        DialogueNode.End {

  /** Stable identifier, unique within the owning {@link DialogueGraph}. */
  String id();

  /** A spoken line delivered by {@code speaker}; click-advances to {@code nextId}. */
  record Line(String id, Speaker speaker, String text, String nextId) implements DialogueNode {
    public Line {
      Objects.requireNonNull(id, "id must not be null");
      Objects.requireNonNull(text, "text must not be null");
      Objects.requireNonNull(nextId, "nextId must not be null");
    }
  }

  /**
   * A branching point: {@code speaker} delivers {@code prompt} and the player picks one of {@code
   * options}.
   */
  record Choice(String id, Speaker speaker, String prompt, List<Option> options)
      implements DialogueNode {
    public Choice {
      Objects.requireNonNull(id, "id must not be null");
      Objects.requireNonNull(prompt, "prompt must not be null");
      Objects.requireNonNull(options, "options must not be null");
      options = List.copyOf(options);
    }
  }

  /**
   * Pauses the conversation until {@code requirementKey} is satisfied, then continues to {@code
   * nextId}.
   *
   * @param requirementKey abstract requirement key resolved by the game
   * @param hintText text shown to the player while waiting (INLINE)
   * @param waitMode whether to keep the widget open or fully suspend
   * @param costsAction when {@code true}, performing the action ticks the game clock by one action
   */
  record Action(
      String id,
      String requirementKey,
      String hintText,
      WaitMode waitMode,
      boolean costsAction,
      String nextId)
      implements DialogueNode {
    public Action {
      Objects.requireNonNull(id, "id must not be null");
      Objects.requireNonNull(requirementKey, "requirementKey must not be null");
      Objects.requireNonNull(hintText, "hintText must not be null");
      Objects.requireNonNull(waitMode, "waitMode must not be null");
      Objects.requireNonNull(nextId, "nextId must not be null");
    }
  }

  /**
   * Fires {@code event} as the conversation passes through, then immediately continues to {@code
   * nextId} (no waiting).
   */
  record Event(String id, DialogueEvent event, String nextId) implements DialogueNode {
    public Event {
      Objects.requireNonNull(id, "id must not be null");
      Objects.requireNonNull(nextId, "nextId must not be null");
    }
  }

  /** Terminal node — ends the conversation. */
  record End(String id) implements DialogueNode {
    public End {
      Objects.requireNonNull(id, "id must not be null");
    }
  }
}
