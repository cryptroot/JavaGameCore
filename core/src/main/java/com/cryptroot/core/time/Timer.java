package com.cryptroot.core.time;

/**
 * A one-shot countdown driven by frame delta — the delta-time replacement for a Unity {@code
 * WaitForSeconds}-style wait.
 *
 * <p>{@link #update(float)} returns {@code true} exactly once, on the tick the countdown crosses
 * zero; further calls return {@code false} until {@link #restart(float)} re-arms it.
 */
public final class Timer {

  private float remaining;
  private boolean fired;

  /** Creates a timer armed for {@code durationSec} seconds. */
  public Timer(float durationSec) {
    restart(durationSec);
  }

  /** Advances the countdown; {@code true} only on the tick it expires. */
  public boolean update(float delta) {
    if (fired) return false;
    remaining -= delta;
    if (remaining <= 0f) {
      remaining = 0f;
      fired = true;
      return true;
    }
    return false;
  }

  /** Re-arms the timer for a new duration. */
  public void restart(float durationSec) {
    remaining = Math.max(0f, durationSec);
    fired = durationSec <= 0f;
  }

  public boolean isExpired() {
    return fired;
  }

  /** Seconds left before expiry (0 once fired). */
  public float remaining() {
    return remaining;
  }
}
