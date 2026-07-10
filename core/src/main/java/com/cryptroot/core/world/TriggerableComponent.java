package com.cryptroot.core.world;

/**
 * Marks a component that can be triggered with a delay, typically to schedule a deferred state
 * reset or action.
 *
 * <p>Implementations are responsible for counting down the delay in their own {@link
 * UpdateComponent#update(float)} and firing the target action when the countdown expires. Calling
 * {@link #trigger(float)} again before the previous countdown finishes restarts the timer from the
 * new value (useful for debouncing repeated interactions).
 *
 * @see com.cryptroot.core.world.component.TimedStateResetComponent
 */
public interface TriggerableComponent extends EntityComponent {

  /**
   * Starts (or restarts) the countdown.
   *
   * @param delaySec time in seconds before the action fires; must be &gt; 0
   */
  void trigger(float delaySec);
}
