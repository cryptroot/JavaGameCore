package com.cryptroot.core.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.grid.Grid;
import com.cryptroot.core.path.Board;

/**
 * Bridges a {@link Collider} against a {@link Grid} + {@link Board} — the "does this shape overlap
 * a blocked tile/cell" check — without either the collider or the grid/board knowing about each
 * other.
 *
 * <p>{@link Board} already models "which cells may not be entered" for {@link
 * com.cryptroot.core.path.Pathfinder}; this class reuses the exact same abstraction for movement
 * collision, so one {@code Board} implementation can serve both pathfinding and physical blocking.
 * What makes a cell blocked (a tile map's edge, an occupied cell, …) is entirely up to that {@link
 * Board} implementation — this class only does the shape/grid geometry.
 */
public final class GridCollisions {

  /**
   * Inset applied to the AABB's max edges before flooring to a cell index, so a box that merely
   * touches a cell's far edge (but does not enter it) is not counted as covering that cell.
   */
  private static final float EDGE_EPSILON = 0.001f;

  private GridCollisions() {}

  /**
   * Returns {@code true} if any cell {@code collider}'s bounds cover is blocked by {@code board},
   * or if the bounds extend outside {@code grid} at all.
   *
   * <p>Leaving the grid always counts as blocked — a unit cannot walk off the edge of the map,
   * independent of whatever {@code board} says about individual cells.
   */
  public static boolean isBlocked(Collider collider, Grid grid, Board board) {
    Rectangle bounds = collider.bounds(new Rectangle());

    int minCol = MathUtils.floor((bounds.x - grid.originX()) / grid.cellWidth());
    int maxCol =
        MathUtils.floor(
            (bounds.x + bounds.width - EDGE_EPSILON - grid.originX()) / grid.cellWidth());
    int minRow = MathUtils.floor((bounds.y - grid.originY()) / grid.cellHeight());
    int maxRow =
        MathUtils.floor(
            (bounds.y + bounds.height - EDGE_EPSILON - grid.originY()) / grid.cellHeight());

    for (int row = minRow; row <= maxRow; row++) {
      for (int col = minCol; col <= maxCol; col++) {
        if (!grid.inBounds(col, row) || board.isBlocked(col, row)) {
          return true;
        }
      }
    }
    return false;
  }
}
