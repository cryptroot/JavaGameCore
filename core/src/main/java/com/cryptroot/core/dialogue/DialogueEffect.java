package com.cryptroot.core.dialogue;

/**
 * A mutation applied to a {@link DialogueBlackboard} when a choice {@link Option} is selected,
 * letting branching decisions change future game state (the effect persists because the blackboard
 * is saved).
 */
@FunctionalInterface
public interface DialogueEffect {

  /** Applies this effect to {@code blackboard}. */
  void apply(DialogueBlackboard blackboard);
}
