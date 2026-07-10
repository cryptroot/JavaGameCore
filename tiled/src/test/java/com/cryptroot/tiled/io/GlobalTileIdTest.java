package com.cryptroot.tiled.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Bit-masking tests for {@link GlobalTileId} flip flags and id extraction. */
class GlobalTileIdTest {

  @Test
  void plainGidHasNoFlagsAndKeepsItsId() {
    int gid = 42;
    assertEquals(42, GlobalTileId.id(gid));
    assertFalse(GlobalTileId.isFlippedHorizontally(gid));
    assertFalse(GlobalTileId.isFlippedVertically(gid));
    assertFalse(GlobalTileId.isFlippedDiagonally(gid));
    assertFalse(GlobalTileId.isEmpty(gid));
  }

  @Test
  void horizontalFlipFlagIsDetectedAndStripped() {
    int gid = 5 | GlobalTileId.FLIP_HORIZONTAL;
    assertTrue(GlobalTileId.isFlippedHorizontally(gid));
    assertFalse(GlobalTileId.isFlippedVertically(gid));
    assertFalse(GlobalTileId.isFlippedDiagonally(gid));
    assertEquals(5, GlobalTileId.id(gid));
  }

  @Test
  void allFlagsCanBeSetTogether() {
    int gid =
        7 | GlobalTileId.FLIP_HORIZONTAL | GlobalTileId.FLIP_VERTICAL | GlobalTileId.FLIP_DIAGONAL;
    assertTrue(GlobalTileId.isFlippedHorizontally(gid));
    assertTrue(GlobalTileId.isFlippedVertically(gid));
    assertTrue(GlobalTileId.isFlippedDiagonally(gid));
    assertEquals(7, GlobalTileId.id(gid));
  }

  @Test
  void emptyCellIsDetected() {
    assertTrue(GlobalTileId.isEmpty(0));
    assertFalse(GlobalTileId.isEmpty(1));
  }
}
