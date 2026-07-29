package com.cryptroot.core.records.sample;

import com.cryptroot.core.records.RecordComponent;
import com.cryptroot.core.records.RecordDimension;

/** What a record's content was obtained from, on the {@link #DIMENSION source} dimension. */
public enum Source implements RecordComponent {
  FOOD("food"),
  DRINK("drink");

  /** The axis these values classify a record along. */
  public static final RecordDimension DIMENSION = RecordDimension.of("source");

  private final String value;

  Source(String value) {
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
