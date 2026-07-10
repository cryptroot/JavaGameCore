package com.cryptroot.core.dialogue;

/**
 * The presentation seam a {@code StoryDirector} drives to show conversations.
 *
 * <p>Implemented by {@link com.cryptroot.core.ui.ConversationWidget} for the real game; tests
 * supply a lightweight fake. Keeping the director coupled to this interface rather than the LibGDX
 * widget lets the director be unit-tested without a graphics context.
 */
public interface DialogueView {

  /** Connects {@code runner} and starts it from its first node. */
  void bind(DialogueRunner runner);

  /** Connects {@code runner} without starting it (used to resume a parked runner). */
  void rebind(DialogueRunner runner);

  /** Clears the view and returns it to the idle (hidden) state. */
  void reset();
}
