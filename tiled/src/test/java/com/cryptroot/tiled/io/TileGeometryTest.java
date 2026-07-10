package com.cryptroot.tiled.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Coordinate-mapping tests for {@link TileGeometry} (top-left/y-down to bottom-left/y-up). */
class TileGeometryTest {

  @Test
  void worldXScalesWithColumn() {
    assertEquals(0f, TileGeometry.worldX(0, 64));
    assertEquals(64f, TileGeometry.worldX(1, 64));
    assertEquals(1216f, TileGeometry.worldX(19, 64));
  }

  @Test
  void worldYFlipsSoTopRowIsHighest() {
    // 24-row map, 64px tiles: the top row (0) sits at the top of the map.
    assertEquals(1472f, TileGeometry.worldY(0, 24, 64));
    assertEquals(64f, TileGeometry.worldY(22, 24, 64));
    // The bottom row (23) sits on the world origin.
    assertEquals(0f, TileGeometry.worldY(23, 24, 64));
  }

  @Test
  void indexIsRowMajor() {
    assertEquals(0, TileGeometry.index(0, 0, 20));
    assertEquals(19, TileGeometry.index(19, 0, 20));
    assertEquals(20, TileGeometry.index(0, 1, 20));
    assertEquals(479, TileGeometry.index(19, 23, 20));
  }
}
