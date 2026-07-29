package com.cryptroot.core.records.sample;

import com.cryptroot.core.records.RecordComponent;
import com.cryptroot.core.records.RecordDimension;

/** Who a record originated from, on the {@link #DIMENSION origin} dimension. */
public enum Origin implements RecordComponent {
  PLAYER("player"),
  NPC("npc"),
  ENEMY("enemy");

  /** The axis these values classify a record along. */
  public static final RecordDimension DIMENSION = RecordDimension.of("origin");

  private final String value;

  Origin(String value) {
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
