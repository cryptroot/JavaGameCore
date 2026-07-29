package com.cryptroot.core.records;

import java.util.OptionalLong;

/**
 * How repeated observations of one record combine into its stored value.
 *
 * <p>Declared per {@link RecordDefinition}, so "HP donated" and "longest jump" can live side by
 * side in the same book, both queryable, both resettable, both reported through the same snapshot:
 *
 * <pre>{@code
 * RecordDefinition.builder()
 *     .components(JUMP, LIGHT_YEARS)
 *     .aggregation(RecordAggregation.MAX)
 *     .name("Longest Hyperjump")
 *     .build();
 * }</pre>
 *
 * <p>Closed on purpose, unlike a {@link RecordDimension}: this is the <em>structure</em> of a
 * record rather than a game's vocabulary. An open strategy could not be persisted, could not be
 * switched over exhaustively, and would let a book hold a value nothing else could reproduce. If
 * you need something beyond these four, compose it from several records.
 *
 * <p>Note that two contracts vary by aggregation, because they only ever made sense for {@link
 * #SUM}: negative observations, and whether an observation of zero is a no-op.
 */
public enum RecordAggregation {

  /**
   * Accumulates — the default, and what a counter means.
   *
   * <p>Rejects negative observations (a record only grows) and treats zero as a no-op, so a caller
   * can forward an event's magnitude without pre-checking it. {@link
   * RecordBookComponent#reset(RecordQuery)} zeroes these rather than removing them.
   */
  SUM,

  /** Keeps the largest observation — "highest damage dealt", "deepest floor reached". */
  MAX,

  /**
   * Keeps the smallest observation — "best time", "fewest turns".
   *
   * <p>The first observation always wins outright: there is no implicit zero to lose against, which
   * is also why {@link RecordBookComponent#reset(RecordQuery)} clears a MIN record instead of
   * zeroing it. A zeroed best time could never be beaten.
   */
  MIN,

  /** Keeps the most recent observation — "current streak", "last score". */
  LAST;

  /**
   * The value to store given the value already stored, if any, and a new {@code observation}.
   *
   * @param previous the stored value, or empty if this record has never been observed
   * @param observation the value just observed
   */
  public long combine(OptionalLong previous, long observation) {
    if (previous.isEmpty()) {
      return observation;
    }
    long stored = previous.getAsLong();
    return switch (this) {
      case SUM -> stored + observation;
      case MAX -> Math.max(stored, observation);
      case MIN -> Math.min(stored, observation);
      case LAST -> observation;
    };
  }

  /** {@code true} if a negative observation is meaningful data rather than a caller bug. */
  public boolean allowsNegativeObservations() {
    return this != SUM;
  }

  /** {@code true} if observing {@code 0} changes nothing and does not create the counter. */
  public boolean treatsZeroAsNoOp() {
    return this == SUM;
  }

  /**
   * {@code true} if a reset removes the record from the book rather than setting it to {@code 0}.
   *
   * <p>Only {@link #SUM} has a meaningful zero; for the others, {@code 0} is an ordinary
   * observation that would corrupt the statistic.
   */
  public boolean clearedByReset() {
    return this != SUM;
  }
}
