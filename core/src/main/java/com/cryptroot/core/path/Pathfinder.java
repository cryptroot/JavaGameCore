package com.cryptroot.core.path;

import com.badlogic.gdx.math.GridPoint2;
import com.cryptroot.core.grid.Grid;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * The single shared grid pathfinding utility: 4-connected A* parameterised by a pluggable {@link
 * PathCostStrategy}, plus a cost-ignoring reachability test.
 *
 * <p>Nothing here knows about any specific game entity — it only sums whatever cost the strategy
 * returns for each walkable cell, where "walkable" means {@code grid.inBounds(col,row) &&
 * !board.isBlocked(col,row)}.
 *
 * <p>Both queries are <em>deterministic</em>: the open set pops the lowest f-score and breaks ties
 * by earliest insertion (neighbours are always expanded in a fixed order), so identical inputs
 * always produce an identical path — a property the tests rely on.
 */
public final class Pathfinder {

  /** Expansion order: +X, -X, +Y, -Y. Fixed for reproducible paths. */
  private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

  private Pathfinder() {}

  /**
   * Lowest-cost 4-connected route from {@code start} to {@code goal} (inclusive of both), or an
   * empty list if the goal is unreachable. Total cost is the sum of the strategy's per-tile cost
   * for every cell entered after the start.
   */
  public static List<GridPoint2> findPath(
      Grid grid, GridPoint2 start, GridPoint2 goal, Board board, PathCostStrategy cost) {
    if (grid == null || cost == null) return List.of();
    Board b = board != null ? board : Board.open();
    if (!passable(grid, b, start) || !passable(grid, b, goal)) return List.of();

    List<GridPoint2> open = new ArrayList<>();
    open.add(start);
    Map<GridPoint2, GridPoint2> cameFrom = new HashMap<>();
    Map<GridPoint2, Float> gScore = new HashMap<>();
    Map<GridPoint2, Float> fScore = new HashMap<>();
    Set<GridPoint2> closed = new HashSet<>();
    gScore.put(start, 0f);
    fScore.put(start, heuristic(start, goal, cost));

    while (!open.isEmpty()) {
      GridPoint2 current = popLowest(open, fScore);
      if (current.equals(goal)) return reconstruct(cameFrom, current);
      closed.add(current);

      for (int[] d : NEIGHBOURS) {
        GridPoint2 next = new GridPoint2(current.x + d[0], current.y + d[1]);
        if (closed.contains(next)) continue;
        if (!passable(grid, b, next)) continue;

        float tentative = gScore.get(current) + cost.tileCost(b, next.x, next.y);
        Float known = gScore.get(next);
        if (known != null && tentative >= known) continue;

        cameFrom.put(next, current);
        gScore.put(next, tentative);
        fScore.put(next, tentative + heuristic(next, goal, cost));
        if (!open.contains(next)) open.add(next);
      }
    }
    return List.of();
  }

  /**
   * {@code true} if any route at all (cost ignored) links {@code start} to {@code goal} given the
   * board's blocked cells. Used to reject placements of hard obstacles that would seal the goal off
   * entirely.
   */
  public static boolean pathExists(Grid grid, GridPoint2 start, GridPoint2 goal, Board board) {
    if (grid == null) return false;
    Board b = board != null ? board : Board.open();
    if (!passable(grid, b, start) || !passable(grid, b, goal)) return false;

    Queue<GridPoint2> frontier = new ArrayDeque<>();
    Set<GridPoint2> seen = new HashSet<>();
    frontier.add(start);
    seen.add(start);

    while (!frontier.isEmpty()) {
      GridPoint2 current = frontier.poll();
      if (current.equals(goal)) return true;
      for (int[] d : NEIGHBOURS) {
        GridPoint2 next = new GridPoint2(current.x + d[0], current.y + d[1]);
        if (seen.contains(next)) continue;
        if (!passable(grid, b, next)) continue;
        seen.add(next);
        frontier.add(next);
      }
    }
    return false;
  }

  private static boolean passable(Grid grid, Board board, GridPoint2 c) {
    return c != null && grid.inBounds(c.x, c.y) && !board.isBlocked(c.x, c.y);
  }

  private static float heuristic(GridPoint2 a, GridPoint2 b, PathCostStrategy cost) {
    int manhattan = Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    return manhattan * Math.max(0f, cost.minTileCost());
  }

  /** Removes and returns the open cell with the lowest f-score (earliest on ties). */
  private static GridPoint2 popLowest(List<GridPoint2> open, Map<GridPoint2, Float> fScore) {
    int bestIndex = 0;
    float best = Float.MAX_VALUE;
    for (int i = 0; i < open.size(); i++) {
      float f = fScore.getOrDefault(open.get(i), Float.MAX_VALUE);
      if (f < best) { // strict: first-inserted wins ties -> deterministic
        best = f;
        bestIndex = i;
      }
    }
    return open.remove(bestIndex);
  }

  private static List<GridPoint2> reconstruct(
      Map<GridPoint2, GridPoint2> cameFrom, GridPoint2 current) {
    List<GridPoint2> path = new ArrayList<>();
    path.add(current);
    GridPoint2 prev;
    while ((prev = cameFrom.get(current)) != null) {
      current = prev;
      path.add(current);
    }
    Collections.reverse(path);
    return path;
  }
}
