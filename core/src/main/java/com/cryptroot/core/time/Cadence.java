package com.cryptroot.core.time;

/**
 * A repeating interval timer for rate-limited actions — the framework form of Unity's {@code
 * attackTimer -= dt; if (t <= 0) { fire; t += 1/rate; }} idiom (e.g. an attack cadence of N hits
 * per second).
 *
 * <p>Usage each frame: {@code cadence.update(delta); while (cadence.consumeReady()) fire();} or,
 * for at-most-one-per-frame semantics, a single {@code if (consumeReady())}. Ready events
 * accumulate across a large delta so no interval is silently dropped (catch-up safe).
 */
public final class Cadence {

  private float interval;
  private float accrued;

  /**
   * @param intervalSec seconds between ready events (e.g. {@code 1f / attacksPerSecond})
   */
  public Cadence(float intervalSec) {
    setInterval(intervalSec);
    reset(); // fresh cadence fires on first consume, matching Unity's attackTimer==0
  }

  /** Accrues elapsed time toward the next ready event. */
  public void update(float delta) {
    if (delta > 0f) accrued += delta;
  }

  /**
   * Consumes one elapsed interval: {@code true} if enough time has accrued for another event
   * (subtracting one interval), else {@code false}. The first call after {@link #reset()} is ready
   * immediately so an engagement lands a hit at once, matching the Unity behaviour.
   */
  public boolean consumeReady() {
    if (accrued >= interval) {
      accrued -= interval;
      return true;
    }
    return false;
  }

  /** Changes the interval; keeps accrued progress. */
  public void setInterval(float intervalSec) {
    this.interval = Math.max(1e-4f, intervalSec);
  }

  /** Resets accrued time to one full interval so the next {@link #consumeReady()} fires. */
  public void reset() {
    accrued = interval;
  }
}
