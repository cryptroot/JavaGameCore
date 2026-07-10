package com.cryptroot.core.render.system;

import com.cryptroot.core.world.DialogueSpeakerComponent;
import com.cryptroot.core.world.WorldEntity;

/**
 * Handles the automatic conversation start when a {@link DialogueSpeakerComponent} entity is
 * clicked.
 *
 * <p>Called by the {@link com.cryptroot.core.render.RenderPipeline} immediately after {@link
 * ClickSystem} to separate click detection from dialogue logic.
 */
public final class DialogueSystem {

  /**
   * If {@code entity} has a {@link DialogueSpeakerComponent} with an active {@link
   * com.cryptroot.core.dialogue.ConversationWidget}, starts the conversation.
   */
  public void onEntityClicked(WorldEntity entity) {
    entity
        .get(DialogueSpeakerComponent.class)
        .flatMap(DialogueSpeakerComponent::conversation)
        .ifPresent(conv -> conv.start());
  }
}
