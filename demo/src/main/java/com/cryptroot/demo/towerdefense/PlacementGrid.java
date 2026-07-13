package com.cryptroot.demo.towerdefense;

import com.badlogic.gdx.math.GridPoint2;
import com.cryptroot.core.grid.Grid;
import com.cryptroot.core.path.Board;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;

/**
 * Game-specific wrapper around a {@link Grid}: which cells belong to the central walkable enemy
 * lane ("floor") versus decorative border, and which cells are currently occupied by a tower.
 *
 * <p>{@code border} is read once from the map's tile layer by the caller (see {@link
 * com.cryptroot.demo.CaveDemoScreen}): enemies only ever spawn ({@link #minSpawnColumn()}..{@link
 * #maxSpawnColumn()} on the bottom row), path, and exit within the floor lane — the border is never
 * part of a route. Towers are <em>not</em> restricted to the floor: {@link #canPlace(int, int)}
 * only rejects a cell that is already occupied or would seal the lane shut end-to-end, so a tower
 * may be placed anywhere on the map, floor or border.
 */
final class PlacementGrid {

  private final Grid grid;
  private final boolean[][] floor; // [col][row] -- true = central walkable lane
  private final boolean[][] occupied; // [col][row] -- true = a tower stands here
  private final int minSpawnColumn;
  private final int maxSpawnColumn;

  /**
   * @param grid the gameplay grid
   * @param border {@code true} (blocked) for cells outside the walkable floor lane — the map's
   *     dark-brown/black border
   */
  PlacementGrid(Grid grid, Board border) {
    this.grid = Objects.requireNonNull(grid, "grid must not be null");
    Objects.requireNonNull(border, "border must not be null");

    int cols = grid.columns();
    int rows = grid.rows();
    this.floor = new boolean[cols][rows];
    for (int col = 0; col < cols; col++) {
      for (int row = 0; row < rows; row++) {
        floor[col][row] = !border.isBlocked(col, row);
      }
    }
    this.occupied = new boolean[cols][rows];

    int min = -1;
    int max = -1;
    for (int col = 0; col < cols; col++) {
      if (floor[col][0]) {
        if (min == -1) min = col;
        max = col;
      }
    }
    if (min == -1) {
      throw new IllegalStateException(
          "Map has no floor cells in the bottom row for enemies to spawn on");
    }
    this.minSpawnColumn = min;
    this.maxSpawnColumn = max;
  }

  Grid grid() {
    return grid;
  }

  /** Smallest column index that is floor in the bottom (spawn) row. */
  int minSpawnColumn() {
    return minSpawnColumn;
  }

  /** Largest column index that is floor in the bottom (spawn) row. */
  int maxSpawnColumn() {
    return maxSpawnColumn;
  }

  boolean isOccupied(int col, int row) {
    return occupied[col][row];
  }

  /** Marks {@code (col,row)} as occupied by a placed tower. */
  void occupy(int col, int row) {
    occupied[col][row] = true;
  }

  /**
   * {@code true} if a tower may be placed at {@code (col,row)}: the cell must be unoccupied and
   * must not, once occupied, seal every route from the bottom of the lane to the top.
   */
  boolean canPlace(int col, int row) {
    if (occupied[col][row]) return false;

    occupied[col][row] = true;
    try {
      return laneConnected();
    } finally {
      occupied[col][row] = false;
    }
  }

  /**
   * A read-only {@link Board} view of the currently occupied cells <em>and</em> the border (both
   * count as impassable for enemies), for {@link com.cryptroot.core.path.Pathfinder}.
   */
  Board board() {
    return (col, row) -> occupied[col][row] || !floor[col][row];
  }

  /**
   * Nearest unoccupied floor cell in {@code row}, searching outward from {@code preferredCol} (0,
   * +1, -1, +2, -2, ...). Returns empty if every floor cell in the row is occupied.
   */
  Optional<GridPoint2> nearestOpenInRow(int row, int preferredCol) {
    int cols = grid.columns();
    for (int offset = 0; offset < cols; offset++) {
      int right = preferredCol + offset;
      if (right < cols && !occupied[right][row] && floor[right][row]) {
        return Optional.of(new GridPoint2(right, row));
      }
      int left = preferredCol - offset;
      if (offset > 0 && left >= 0 && !occupied[left][row] && floor[left][row]) {
        return Optional.of(new GridPoint2(left, row));
      }
    }
    return Optional.empty();
  }

  /**
   * Multi-source BFS across the current {@link #board()}: {@code true} if any open floor cell at
   * the bottom row can still reach any open floor cell at the top row. Cheaper than repeatedly
   * calling {@link com.cryptroot.core.path.Pathfinder#pathExists} for every bottom/top pair, so
   * this is safe to call every time the placement preview's hovered cell changes.
   */
  private boolean laneConnected() {
    int cols = grid.columns();
    int rows = grid.rows();
    int top = rows - 1;
    Board board = board();

    boolean[][] seen = new boolean[cols][rows];
    ArrayDeque<GridPoint2> frontier = new ArrayDeque<>();
    for (int col = 0; col < cols; col++) {
      if (!board.isBlocked(col, 0)) {
        seen[col][0] = true;
        frontier.add(new GridPoint2(col, 0));
      }
    }

    int[][] deltas = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    while (!frontier.isEmpty()) {
      GridPoint2 current = frontier.poll();
      if (current.y == top) return true;
      for (int[] d : deltas) {
        int nx = current.x + d[0];
        int ny = current.y + d[1];
        if (!grid.inBounds(nx, ny) || seen[nx][ny] || board.isBlocked(nx, ny)) continue;
        seen[nx][ny] = true;
        frontier.add(new GridPoint2(nx, ny));
      }
    }
    return false;
  }
}
