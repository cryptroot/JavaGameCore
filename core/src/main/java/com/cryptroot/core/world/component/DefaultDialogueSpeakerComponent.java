package com.cryptroot.core.world.component;

import com.cryptroot.core.dialogue.Speaker;
import com.cryptroot.core.ui.ConversationWidget;
import com.cryptroot.core.world.DialogueSpeakerComponent;
import java.util.Optional;

/**
 * Associates a {@link Speaker} (and optional {@link ConversationWidget}) with an entity.
 *
 * <p>When {@link #conversation()} is present, {@link com.cryptroot.core.world.WorldEntityLayer}
 * automatically calls {@link ConversationWidget#start()} when the entity is clicked — no manual
 * listener wiring is required for this common case.
 *
 * <pre>{@code
 * entity.with(DialogueSpeakerComponent.class,
 *     new DefaultDialogueSpeakerComponent(florenceSpeaker, conversationWidget));
 * }</pre>
 */
public final class DefaultDialogueSpeakerComponent implements DialogueSpeakerComponent {

  private final Speaker speaker;
  private final ConversationWidget conversation;

  /** Entity has a speaker but does not auto-start a conversation on click. */
  public DefaultDialogueSpeakerComponent(Speaker speaker) {
    this.speaker = speaker;
    this.conversation = null;
  }

  /** Entity has a speaker and auto-starts {@code conversation} when clicked. */
  public DefaultDialogueSpeakerComponent(Speaker speaker, ConversationWidget conversation) {
    this.speaker = speaker;
    this.conversation = conversation;
  }

  @Override
  public Speaker speaker() {
    return speaker;
  }

  @Override
  public Optional<ConversationWidget> conversation() {
    return Optional.ofNullable(conversation);
  }
}
