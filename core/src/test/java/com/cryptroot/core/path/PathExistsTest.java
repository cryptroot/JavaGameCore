package com.cryptroot.core.path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.GridPoint2;
import com.cryptroot.core.grid.Grid;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PathExistsTest {

  private static Grid grid(int cols, int rows) {
    return new Grid(0f, 0f, 1f, cols, rows);
  }

  private static Board blocking(GridPoint2... blocked) {
    Set<Long> set = new HashSet<>();
    for (GridPoint2 c : blocked) set.add((((long) c.x) << 32) ^ (c.y & 0xffffffffL));
    return (col, row) -> set.contains((((long) col) << 32) ^ (row & 0xffffffffL));
  }

  private static GridPoint2 p(int x, int y) {
    return new GridPoint2(x, y);
  }

  @Test
  void connectedWhenGapLeftOpen() {
    // Wall across column 2 but with a gap at row 4 -> still reachable.
    Board board = blocking(p(2, 0), p(2, 1), p(2, 2), p(2, 3));
    assertTrue(Pathfinder.pathExists(grid(5, 5), p(0, 0), p(4, 0), board));
  }

  @Test
  void sealedGoalIsNotReachable() {
    // Full wall across column 2 -> goal side is sealed off.
    Board board = blocking(p(2, 0), p(2, 1), p(2, 2), p(2, 3), p(2, 4));
    assertFalse(Pathfinder.pathExists(grid(5, 5), p(0, 0), p(4, 0), board));
  }

  @Test
  void blockedEndpointIsNotReachable() {
    Board board = blocking(p(4, 0));
    assertFalse(Pathfinder.pathExists(grid(5, 5), p(0, 0), p(4, 0), board));
  }

  @Test
  void ignoresCostUsesPlainConnectivity() {
    // pathExists takes no strategy at all; open board is trivially connected.
    assertTrue(Pathfinder.pathExists(grid(3, 3), p(0, 0), p(2, 2), Board.open()));
  }
}
