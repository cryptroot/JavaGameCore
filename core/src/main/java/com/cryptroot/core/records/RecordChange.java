package com.cryptroot.core.records;

import java.util.Objects;

/**
 * One record's stored value changing, as emitted by {@link RecordBookComponent#onRecorded()}.
 *
 * <p>Carries both sides of the change rather than just the key, so a listener can act on a
 * threshold being crossed without re-reading the book — which is what an achievement or a UI toast
 * needs:
 *
 * <pre>{@code
 * book.onRecorded().connect(change -> {
 *   if (change.previous() < 50 && change.current() >= 50) {
 *     unlock("trailblazer");
 *   }
 * });
 * }</pre>
 *
 * <p>Only emitted when the stored value actually moved, so {@code previous != current} always holds
 * and {@link #delta()} is never zero. A {@link RecordAggregation#MAX} observation that fails to
 * beat the record is silent.
 *
 * @param key the record that changed, never {@code null}
 * @param previous the value before the change — {@code 0} if the record had never been observed,
 *     matching what {@link RecordBookComponent#value} would have returned
 * @param current the value now stored
 */
public record RecordChange(RecordKey key, long previous, long current) {

  /**
   * @throws NullPointerException if {@code key} is {@code null}
   */
  public RecordChange {
    Objects.requireNonNull(key, "key must not be null");
  }

  /** How far the value moved; negative when the record went down, never zero. */
  public long delta() {
    return current - previous;
  }
}
