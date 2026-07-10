package com.cryptroot.core.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import com.cryptroot.core.grid.Grid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PathfinderTest {

  private static Grid grid(int cols, int rows) {
    return new Grid(0f, 0f, 1f, cols, rows);
  }

  private static Board blocking(GridPoint2... blocked) {
    Set<Long> set = new HashSet<>();
    for (GridPoint2 c : blocked) set.add(key(c.x, c.y));
    return (col, row) -> set.contains(key(col, row));
  }

  private static long key(int col, int row) {
    return (((long) col) << 32) ^ (row & 0xffffffffL);
  }

  private static GridPoint2 p(int x, int y) {
    return new GridPoint2(x, y);
  }

  @Test
  void startEqualsGoalIsSingleCell() {
    List<GridPoint2> path =
        Pathfinder.findPath(grid(5, 5), p(2, 2), p(2, 2), Board.open(), PathCostStrategy.uniform());
    assertEquals(List.of(p(2, 2)), path);
  }

  @Test
  void straightLineHasOptimalLength() {
    List<GridPoint2> path =
        Pathfinder.findPath(grid(5, 1), p(0, 0), p(4, 0), Board.open(), PathCostStrategy.uniform());
    assertEquals(5, path.size()); // inclusive of both ends
    assertEquals(p(0, 0), path.get(0));
    assertEquals(p(4, 0), path.get(path.size() - 1));
    assertContiguous(path);
  }

  @Test
  void routesAroundBlockedCell() {
    // Wall across the middle column except the top row forces a detour.
    Board board = blocking(p(2, 0), p(2, 1), p(2, 2), p(2, 3));
    List<GridPoint2> path =
        Pathfinder.findPath(grid(5, 5), p(0, 0), p(4, 0), board, PathCostStrategy.uniform());
    assertFalse(path.isEmpty());
    assertEquals(p(0, 0), path.get(0));
    assertEquals(p(4, 0), path.get(path.size() - 1));
    assertContiguous(path);
    for (GridPoint2 c : path) {
      assertFalse(board.isBlocked(c.x, c.y), "path must avoid blocked cells");
    }
  }

  @Test
  void unreachableGoalReturnsEmpty() {
    // Seal goal (4,0) behind a full wall.
    Board board = blocking(p(3, 0), p(3, 1), p(3, 2), p(3, 3), p(3, 4));
    List<GridPoint2> path =
        Pathfinder.findPath(grid(5, 5), p(0, 0), p(4, 0), board, PathCostStrategy.uniform());
    assertTrue(path.isEmpty());
  }

  @Test
  void isDeterministic() {
    Grid g = grid(6, 6);
    Board board = blocking(p(3, 2), p(3, 3));
    List<GridPoint2> a =
        Pathfinder.findPath(g, p(0, 0), p(5, 5), board, PathCostStrategy.uniform());
    List<GridPoint2> b =
        Pathfinder.findPath(g, p(0, 0), p(5, 5), board, PathCostStrategy.uniform());
    assertEquals(a, b, "same inputs must produce the identical path");
  }

  @Test
  void costStrategyBendsRouteAwayFromExpensiveTiles() {
    // A 3-wide, 2-tall grid. A* from (0,0) to (2,0). Make the direct middle
    // tile (1,0) very expensive; the cheaper route detours through row 1.
    Grid g = grid(3, 2);
    Set<Long> expensive = Set.of(key(1, 0));
    PathCostStrategy cost =
        new PathCostStrategy() {
          @Override
          public float tileCost(Board b, int col, int row) {
            return expensive.contains(key(col, row)) ? 50f : 1f;
          }

          @Override
          public float minTileCost() {
            return 1f;
          }
        };

    List<GridPoint2> avoid = Pathfinder.findPath(g, p(0, 0), p(2, 0), Board.open(), cost);
    assertTrue(avoid.contains(p(1, 1)), "should detour through the cheap upper row");
    assertFalse(avoid.contains(p(1, 0)), "should avoid the expensive middle tile");

    // With uniform cost it takes the direct 3-cell line through (1,0).
    List<GridPoint2> direct =
        Pathfinder.findPath(g, p(0, 0), p(2, 0), Board.open(), PathCostStrategy.uniform());
    assertEquals(3, direct.size());
    assertTrue(direct.contains(p(1, 0)));
  }

  private static void assertContiguous(List<GridPoint2> path) {
    for (int i = 1; i < path.size(); i++) {
      GridPoint2 a = path.get(i - 1), b = path.get(i);
      int step = Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
      assertEquals(1, step, "consecutive cells must be 4-adjacent");
    }
  }
}
