package com.cryptroot.core.dialogue;

/**
 * Controls how a conversation behaves while an {@link DialogueNode.Action} waits for its
 * requirement to be satisfied.
 */
public enum WaitMode {

  /**
   * Keep the conversation widget open, showing the action's hint, while the player performs the
   * required action. The conversation auto-resumes the moment the requirement is met. Use for "do
   * this now" tutorial beats.
   */
  INLINE,

  /**
   * Close the conversation entirely and return control to the game. The runner is parked until the
   * requirement is met — possibly across multiple days — then the conversation auto-resumes. Use
   * for "come back later" beats (e.g. <em>wait for Day 2</em>).
   */
  SUSPEND
}
