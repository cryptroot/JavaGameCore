package com.cryptroot.core.records.sample;

import com.cryptroot.core.records.RecordComponent;
import com.cryptroot.core.records.RecordDimension;

/** What a record is about, on the {@link #DIMENSION content} dimension. */
public enum Content implements RecordComponent {
  BLOOD("blood"),
  WATER("water"),
  UNKNOWN("unknown");

  /** The axis these values classify a record along. */
  public static final RecordDimension DIMENSION = RecordDimension.of("content");

  private final String value;

  Content(String value) {
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
