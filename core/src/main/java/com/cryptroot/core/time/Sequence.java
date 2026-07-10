package com.cryptroot.core.time;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A tiny coroutine-like sequencer: an ordered list of steps that advance under a per-frame delta.
 * It replaces Unity {@code IEnumerator} coroutines such as {@code HitFlashRoutine}, {@code
 * FlashMessageRoutine} and the knight-spawn {@code ResolveRoutine} ({@code yield return new
 * WaitForSeconds(...)}, {@code while (!done) yield return null}, interleaved instantaneous
 * actions).
 *
 * <p>Build with {@link #builder()}, then call {@link #update(float)} every frame (directly, via a
 * {@link SequenceComponent} on an entity, or via a {@link Scheduler} for world-scoped routines).
 * Instantaneous {@code run} steps and satisfied waits chain within a single tick, and leftover
 * delta carries into the next step so timing does not drift.
 */
public final class Sequence {

  private final List<Step> steps;
  private int index;
  private boolean cancelled;

  private Sequence(List<Step> steps) {
    this.steps = steps;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Advances as far as this tick's {@code delta} allows. */
  public void update(float delta) {
    if (isDone()) return;
    float remaining = Math.max(0f, delta);
    while (index < steps.size()) {
      float leftover = steps.get(index).consume(remaining);
      if (leftover < 0f) return; // current step still blocking
      index++;
      remaining = leftover;
    }
  }

  /** {@code true} once every step has completed, or after {@link #cancel()}. */
  public boolean isDone() {
    return cancelled || index >= steps.size();
  }

  /** Halts the sequence permanently; {@link #isDone()} becomes {@code true}. */
  public void cancel() {
    cancelled = true;
  }

  // -------------------------------------------------------------------------

  /**
   * A single step. Returns leftover delta (&gt;= 0) when it completes this call, or a negative
   * value when it is still blocking.
   */
  private interface Step {
    float consume(float delta);
  }

  public static final class Builder {
    private final List<Step> steps = new ArrayList<>();

    /** Blocks for {@code seconds} of accumulated delta. */
    public Builder waitSeconds(float seconds) {
      steps.add(
          new Step() {
            float remaining = Math.max(0f, seconds);

            @Override
            public float consume(float delta) {
              remaining -= delta;
              return remaining <= 0f ? -remaining : -1f; // leftover, else blocked
            }
          });
      return this;
    }

    /** Blocks until {@code condition} is {@code true} (checked each tick). */
    public Builder waitUntil(BooleanSupplier condition) {
      steps.add(delta -> condition.getAsBoolean() ? delta : -1f);
      return this;
    }

    /** Runs {@code action} instantaneously, then advances. */
    public Builder run(Runnable action) {
      steps.add(
          delta -> {
            action.run();
            return delta;
          });
      return this;
    }

    public Sequence build() {
      return new Sequence(new ArrayList<>(steps));
    }
  }
}
