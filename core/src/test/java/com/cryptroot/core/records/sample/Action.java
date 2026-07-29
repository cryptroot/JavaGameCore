package com.cryptroot.core.records.sample;

import com.cryptroot.core.records.RecordComponent;
import com.cryptroot.core.records.RecordDimension;

/** What was done to a record's content, on the {@link #DIMENSION action} dimension. */
public enum Action implements RecordComponent {
  USED("used"),
  EATEN("eaten");

  /** The axis these values classify a record along. */
  public static final RecordDimension DIMENSION = RecordDimension.of("action");

  private final String value;

  Action(String value) {
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
