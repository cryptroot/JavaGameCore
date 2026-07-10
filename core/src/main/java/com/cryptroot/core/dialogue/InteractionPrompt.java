package com.cryptroot.core.dialogue;

/**
 * A non-blocking on-screen hint that tells the player an optional interaction is available — e.g.
 * <em>"Click on Laura to chat"</em>.
 *
 * <p>The prompt is purely advisory: it never grabs input or forces the player to act. It is driven
 * by an {@link AmbientConversationDirector}, which shows it while an ambient conversation is armed
 * and hides it while the conversation is playing.
 *
 * <p>Kept free of LibGDX types so directors can be unit-tested with a fake; the real game backs it
 * with {@code LabelInteractionPrompt}.
 */
public interface InteractionPrompt {

  /** Shows (or updates) the prompt with the given text. */
  void show(String text);

  /** Hides the prompt. */
  void hide();

  /** A no-op prompt, useful when no on-screen hint is desired. */
  InteractionPrompt NONE =
      new InteractionPrompt() {
        @Override
        public void show(String text) {}

        @Override
        public void hide() {}
      };
}
