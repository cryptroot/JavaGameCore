package com.cryptroot.demo.towerdefense;

import com.badlogic.gdx.math.GridPoint2;
import com.cryptroot.core.grid.Grid;
import com.cryptroot.core.path.Board;
import java.util.Objects;
import java.util.Optional;

/**
 * Game-specific wrapper around a {@link Grid}: which cells are placeable and which are currently
 * occupied by a tower.
 *
 * <p>Only the leftmost and rightmost columns are placeable ("tiles at the edge of the screen"),
 * matching {@link Grid}'s bottom-left/y-up convention. Restricting placement to the side columns
 * (rather than the top/bottom rows too) guarantees the bottom (spawn) and top (goal) rows always
 * keep at least one open interior column, so enemies can never be sealed off by tower placement.
 */
final class PlacementGrid {

  private final Grid grid;
  private final boolean[][] occupied; // [col][row]

  PlacementGrid(Grid grid) {
    this.grid = Objects.requireNonNull(grid, "grid must not be null");
    this.occupied = new boolean[grid.columns()][grid.rows()];
  }

  Grid grid() {
    return grid;
  }

  /** {@code true} if {@code col} is one of the two placeable border columns. */
  boolean isEdgeColumn(int col) {
    return col == 0 || col == grid.columns() - 1;
  }

  boolean isOccupied(int col, int row) {
    return occupied[col][row];
  }

  /** Marks {@code (col,row)} as occupied by a placed tower. */
  void occupy(int col, int row) {
    occupied[col][row] = true;
  }

  /**
   * A read-only {@link Board} view of the currently occupied cells, for {@link
   * com.cryptroot.core.path.Pathfinder}.
   */
  Board board() {
    return (col, row) -> occupied[col][row];
  }

  /**
   * Nearest unoccupied cell in {@code row}, searching outward from {@code preferredCol} (0, +1, -1,
   * +2, -2, ...). Returns empty if every cell in the row is occupied.
   */
  Optional<GridPoint2> nearestOpenInRow(int row, int preferredCol) {
    int cols = grid.columns();
    for (int offset = 0; offset < cols; offset++) {
      int right = preferredCol + offset;
      if (right < cols && !occupied[right][row]) return Optional.of(new GridPoint2(right, row));
      int left = preferredCol - offset;
      if (offset > 0 && left >= 0 && !occupied[left][row]) {
        return Optional.of(new GridPoint2(left, row));
      }
    }
    return Optional.empty();
  }
}
