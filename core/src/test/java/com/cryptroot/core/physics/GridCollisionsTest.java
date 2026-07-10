package com.cryptroot.core.physics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.grid.Grid;
import com.cryptroot.core.path.Board;
import org.junit.jupiter.api.Test;

class GridCollisionsTest {

  /** A fixed-bounds fake collider, independent of any {@code PositionComponent}/entity. */
  private static Collider fixedAt(float x, float y, float w, float h) {
    return out -> {
      out.set(x, y, w, h);
      return out;
    };
  }

  @Test
  void openCellInsideGridIsNotBlocked() {
    Grid grid = new Grid(0f, 0f, 16f, 4, 4);
    Collider collider = fixedAt(16f, 16f, 16f, 16f); // exactly cell (1,1)
    assertFalse(GridCollisions.isBlocked(collider, grid, Board.open()));
  }

  @Test
  void blockedCellIsDetected() {
    Grid grid = new Grid(0f, 0f, 16f, 4, 4);
    Board board = (col, row) -> col == 1 && row == 1;
    Collider collider = fixedAt(16f, 16f, 16f, 16f); // exactly cell (1,1)
    assertTrue(GridCollisions.isBlocked(collider, grid, board));
  }

  @Test
  void straddlingCellsCatchesABlockedNeighbour() {
    Grid grid = new Grid(0f, 0f, 16f, 4, 4);
    Board board = (col, row) -> col == 2 && row == 1; // only cell (2,1) blocked
    // Box spans x=[24,40) -> cols 1 and 2 at row 1 (y=[16,32)); corners alone would miss (2,1).
    Collider collider = fixedAt(24f, 16f, 16f, 16f);
    assertTrue(GridCollisions.isBlocked(collider, grid, board));
  }

  @Test
  void leavingGridBoundsCountsAsBlockedInEveryDirection() {
    Grid grid = new Grid(0f, 0f, 16f, 4, 4); // world spans [0,64) x [0,64)
    Board board = Board.open();

    assertTrue(
        GridCollisions.isBlocked(fixedAt(-8f, 20f, 16f, 16f), grid, board), "off the left edge");
    assertTrue(
        GridCollisions.isBlocked(fixedAt(60f, 20f, 16f, 16f), grid, board), "off the right edge");
    assertTrue(
        GridCollisions.isBlocked(fixedAt(20f, -8f, 16f, 16f), grid, board), "off the bottom edge");
    assertTrue(
        GridCollisions.isBlocked(fixedAt(20f, 60f, 16f, 16f), grid, board), "off the top edge");
  }

  @Test
  void fullyInsideOpenGridIsNotBlocked() {
    Grid grid = new Grid(0f, 0f, 16f, 4, 4);
    Collider collider = fixedAt(0f, 0f, 64f, 64f); // exactly fills the grid
    assertFalse(GridCollisions.isBlocked(collider, grid, Board.open()));
  }
}
