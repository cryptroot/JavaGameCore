package com.cryptroot.core.world;

import com.cryptroot.core.dialogue.Speaker;
import com.cryptroot.core.ui.ConversationWidget;
import java.util.Optional;

/**
 * Component that associates a dialogue {@link Speaker} with an entity.
 *
 * <p>When an entity with this component is clicked, {@link WorldEntityLayer} automatically starts
 * {@link #conversation()} if present — no manual wiring required in the screen.
 */
public interface DialogueSpeakerComponent extends EntityComponent {
  Speaker speaker();

  /** The conversation to start when this entity is clicked, or empty if none. */
  Optional<ConversationWidget> conversation();
}
