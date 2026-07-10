package com.cryptroot.tiled.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import org.junit.jupiter.api.Test;

class TileGeometryInverseTest {

  @Test
  void inverseRoundTripsForwardMapping() {
    int mapW = 5, mapH = 6, tw = 64, th = 64;
    for (int col = 0; col < mapW; col++) {
      for (int row = 0; row < mapH; row++) {
        // A point at the center of the tile quad.
        float wx = TileGeometry.worldX(col, tw) + tw / 2f;
        float wy = TileGeometry.worldY(row, mapH, th) + th / 2f;
        assertEquals(col, TileGeometry.columnAt(wx, tw));
        assertEquals(row, TileGeometry.rowAt(wy, mapH, th), "row must survive the y-flip");
      }
    }
  }

  @Test
  void topRowMapsToHighWorldY() {
    int mapH = 6, th = 64;
    // Tiled row 0 (top) sits at the highest world Y band.
    float topBottomEdge = TileGeometry.worldY(0, mapH, th); // (mapH-1)*th
    assertEquals((mapH - 1) * th, topBottomEdge, 1e-4f);
    assertEquals(0, TileGeometry.rowAt(topBottomEdge + 1f, mapH, th));
    // Tiled bottom row is at world Y 0.
    assertEquals(mapH - 1, TileGeometry.rowAt(0f, mapH, th));
  }

  @Test
  void cellAtRejectsOutOfBounds() {
    GridPoint2 out = new GridPoint2(-9, -9);
    assertFalse(TileGeometry.cellAt(-1f, 10f, 5, 6, 64, 64, out));
    assertEquals(-9, out.x, "out untouched on OOB");
    assertTrue(TileGeometry.cellAt(70f, 70f, 5, 6, 64, 64, out));
    assertEquals(1, out.x);
  }
}
