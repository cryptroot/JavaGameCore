package com.cryptroot.core.records.sample;

import com.cryptroot.core.records.RecordComponent;
import com.cryptroot.core.records.RecordDimension;

/** The condition of a record's content, on the {@link #DIMENSION quality} dimension. */
public enum Quality implements RecordComponent {
  NORMAL("normal"),
  SPOILED("spoiled");

  /** The axis these values classify a record along. */
  public static final RecordDimension DIMENSION = RecordDimension.of("quality");

  private final String value;

  Quality(String value) {
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
