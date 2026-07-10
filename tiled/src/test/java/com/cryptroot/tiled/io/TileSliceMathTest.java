package com.cryptroot.tiled.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Tests for {@link TileSliceMath} tile rectangle computation. */
class TileSliceMathTest {

  @Test
  void slicesCaveTilesetGrid() {
    // Cave_Tilemap.tsx: 2 columns, 64px tiles, no margin/spacing.
    assertEquals(new TileRect(0, 0, 64, 64), TileSliceMath.rect(0, 2, 64, 64, 0, 0));
    assertEquals(new TileRect(64, 0, 64, 64), TileSliceMath.rect(1, 2, 64, 64, 0, 0));
    assertEquals(new TileRect(0, 64, 64, 64), TileSliceMath.rect(2, 2, 64, 64, 0, 0));
    assertEquals(new TileRect(64, 64, 64, 64), TileSliceMath.rect(3, 2, 64, 64, 0, 0));
  }

  @Test
  void appliesMarginAndSpacing() {
    // 3 columns, 16px tiles, 1px margin, 2px spacing.
    assertEquals(new TileRect(1, 1, 16, 16), TileSliceMath.rect(0, 3, 16, 16, 1, 2));
    assertEquals(new TileRect(19, 1, 16, 16), TileSliceMath.rect(1, 3, 16, 16, 1, 2));
    assertEquals(new TileRect(37, 1, 16, 16), TileSliceMath.rect(2, 3, 16, 16, 1, 2));
    // Wraps to the next row.
    assertEquals(new TileRect(1, 19, 16, 16), TileSliceMath.rect(3, 3, 16, 16, 1, 2));
  }

  @Test
  void rejectsNonPositiveColumns() {
    assertThrows(IllegalArgumentException.class, () -> TileSliceMath.rect(0, 0, 16, 16, 0, 0));
  }
}
