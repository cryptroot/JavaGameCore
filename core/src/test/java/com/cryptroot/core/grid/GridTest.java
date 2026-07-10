package com.cryptroot.core.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GridTest {

  // Mirrors the Unity GridManager defaults: origin (0,-24), 4-unit cells, 5x6.
  private static Grid cave() {
    return new Grid(0f, -24f, 4f, 5, 6);
  }

  @Test
  void cellCenterAndRoundTrip() {
    Grid g = cave();
    Vector2 c = g.cellToWorld(2, 0); // lane column, spawn row
    assertEquals(0f + 2.5f * 4f, c.x, 1e-4f); // 10
    assertEquals(-24f + 0.5f * 4f, c.y, 1e-4f); // -22

    GridPoint2 out = new GridPoint2();
    assertTrue(g.worldToCell(c.x, c.y, out));
    assertEquals(2, out.x);
    assertEquals(0, out.y);
  }

  @Test
  void floorSemanticsOnCellEdges() {
    Grid g = cave();
    GridPoint2 out = new GridPoint2();
    // Bottom-left corner of cell (0,0) belongs to (0,0).
    assertTrue(g.worldToCell(0f, -24f, out));
    assertEquals(0, out.x);
    assertEquals(0, out.y);
    // Just inside the boundary between col 0 and col 1.
    assertTrue(g.worldToCell(3.999f, -24f, out));
    assertEquals(0, out.x);
    assertTrue(g.worldToCell(4.001f, -24f, out));
    assertEquals(1, out.x);
  }

  @Test
  void outOfBoundsReturnsEmptyAndDoesNotWriteOut() {
    Grid g = cave();
    assertFalse(g.inBounds(-1, 0));
    assertFalse(g.inBounds(5, 0));
    assertFalse(g.inBounds(0, 6));

    GridPoint2 out = new GridPoint2(-99, -99);
    assertFalse(g.worldToCell(-1f, 0f, out)); // left of origin
    assertEquals(-99, out.x, "out must be untouched on OOB");
    assertEquals(-99, out.y);

    Optional<GridPoint2> none = g.worldToCell(1000f, 1000f);
    assertTrue(none.isEmpty());
  }

  @Test
  void nonSquareCells() {
    Grid g = new Grid(0f, 0f, 10f, 4f, 3, 2);
    assertEquals(30f, g.worldWidth(), 1e-4f);
    assertEquals(8f, g.worldHeight(), 1e-4f);
    Vector2 c = g.cellToWorld(2, 1);
    assertEquals(25f, c.x, 1e-4f); // (2+0.5)*10
    assertEquals(6f, c.y, 1e-4f); // (1+0.5)*4
  }

  @Test
  void inBoundsCorners() {
    Grid g = cave();
    assertTrue(g.inBounds(0, 0));
    assertTrue(g.inBounds(4, 5));
    assertTrue(g.inBounds(new GridPoint2(4, 5)));
    assertFalse(g.inBounds((GridPoint2) null));
  }
}
