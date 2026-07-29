package com.cryptroot.core.records.sample;

import com.cryptroot.core.records.RecordComponent;
import com.cryptroot.core.records.RecordDimension;

/**
 * How a record is counted, on the {@link #DIMENSION measure} dimension.
 *
 * <p>{@link #TIMES} counts occurrences (one increment per event), {@link #AMOUNT} accumulates a
 * quantity (increment by the size of the event, e.g. HP donated).
 */
public enum Measure implements RecordComponent {
  AMOUNT("amount"),
  TIMES("times");

  /** The axis these values classify a record along. */
  public static final RecordDimension DIMENSION = RecordDimension.of("measure");

  private final String value;

  Measure(String value) {
    this.value = value;
  }

  @Override
  public RecordDimension dimension() {
    return DIMENSION;
  }

  @Override
  public String value() {
    return value;
  }
}
