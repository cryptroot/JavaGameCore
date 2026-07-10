package com.cryptroot.core.dialogue;

import com.cryptroot.core.event.EventBus;

/**
 * A side-effect fired by an {@link DialogueNode.Event} as a conversation traverses it.
 * Implementations typically publish a game event onto the {@link EventBus} so decoupled systems can
 * react.
 */
@FunctionalInterface
public interface DialogueEvent {

  /** Fires this event, given the game-wide {@link EventBus}. */
  void fire(EventBus bus);
}
