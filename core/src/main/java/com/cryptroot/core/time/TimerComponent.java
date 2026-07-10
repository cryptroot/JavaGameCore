package com.cryptroot.core.time;

import com.cryptroot.core.event.Signal0;
import com.cryptroot.core.world.TriggerableComponent;
import com.cryptroot.core.world.UpdateComponent;

/**
 * The concrete {@link TriggerableComponent} the framework has long referenced but never shipped: a
 * delta-driven countdown that fires {@link #onExpire()} once when it elapses. Ticked by the
 * standard {@code UpdateSystem}.
 *
 * <p>{@link #trigger(float)} (re)starts the countdown — calling it again before expiry restarts
 * from the new delay (debounce). Typical use: schedule a deferred state reset or a one-shot action
 * after a delay.
 */
public final class TimerComponent implements UpdateComponent, TriggerableComponent {

  private final Signal0 onExpire = new Signal0();
  private Timer timer; // null until first trigger()

  /** Starts (or restarts) the countdown. */
  @Override
  public void trigger(float delaySec) {
    if (timer == null) timer = new Timer(delaySec);
    else timer.restart(delaySec);
  }

  @Override
  public void update(float delta) {
    if (timer != null && timer.update(delta)) {
      onExpire.emit();
    }
  }

  /** Fires once each time an armed countdown reaches zero. */
  public Signal0 onExpire() {
    return onExpire;
  }
}
