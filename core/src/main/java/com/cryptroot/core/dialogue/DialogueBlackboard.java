package com.cryptroot.core.dialogue;

/**
 * Read/write access to the persistent story state a {@link DialogueRunner} consults while
 * traversing a {@link DialogueGraph}.
 *
 * <p>Conditions ({@link DialogueCondition}) read flags/vars to gate branches; effects ({@link
 * DialogueEffect}) write them when a choice is selected. The runner also queries {@link
 * #requirementSatisfied(String)} when it reaches an {@link DialogueNode.Action} to decide whether
 * to pause.
 *
 * <p>Core ships a trivial in-memory {@link MapDialogueBlackboard}; games supply a save-backed
 * implementation.
 */
public interface DialogueBlackboard {

  /** Returns the boolean flag for {@code key}, or {@code false} if unset. */
  boolean flag(String key);

  /** Sets the boolean flag for {@code key}. */
  void setFlag(String key, boolean value);

  /** Returns the integer variable for {@code key}, or {@code 0} if unset. */
  int var(String key);

  /** Sets the integer variable for {@code key}. */
  void setVar(String key, int value);

  /**
   * Returns {@code true} when the gameplay requirement identified by {@code key} is currently
   * satisfied. Used by {@link DialogueNode.Action} nodes to decide whether to pass through or
   * pause.
   */
  boolean requirementSatisfied(String key);
}
