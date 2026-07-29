package com.cryptroot.core.records.sample;

import com.cryptroot.core.records.RecordComponent;
import com.cryptroot.core.records.RecordDimension;

/**
 * Where a record was accumulated, on the {@link #DIMENSION context} dimension.
 *
 * <p>{@link #BATTLE} is the per-battle context: those counters are the ones a battler zeroes at the
 * start of a fight via {@link RecordBookComponent#reset(RecordComponent)}, while every other
 * context accumulates for the entity's lifetime.
 */
public enum Context implements RecordComponent {
  MAP("map"),
  BATTLE("battle"),
  BANK("bank");

  /** The axis these values classify a record along. */
  public static final RecordDimension DIMENSION = RecordDimension.of("context");

  private final String value;

  Context(String value) {
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
