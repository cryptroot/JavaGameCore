package com.cryptroot.core.dialogue;

/**
 * A predicate over a {@link DialogueBlackboard} used to gate a choice {@link Option} (e.g. only
 * enable an option when a flag is set).
 */
@FunctionalInterface
public interface DialogueCondition {

  /** Returns {@code true} when the option this condition guards is enabled. */
  boolean test(DialogueBlackboard blackboard);
}
