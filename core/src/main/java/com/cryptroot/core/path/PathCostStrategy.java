package com.cryptroot.core.path;

/**
 * Per-tile traversal cost for {@link Pathfinder#findPath}. Swapping the strategy is the entire
 * routing-behaviour override hook: the pathfinder never changes, only the numbers it sums. Costs
 * are only ever queried for cells a unit is actually allowed to enter (a {@link Board} already
 * excludes blocked cells).
 *
 * <p>Not a {@code @FunctionalInterface}: it has two abstract methods, and {@link #minTileCost()}
 * must be a genuine lower bound on {@link #tileCost} so the A* heuristic (manhattan distance ×
 * {@code minTileCost}) stays admissible.
 */
public interface PathCostStrategy {

  /** Cost to step onto cell {@code (col,row)}; must be &gt; 0. */
  float tileCost(Board board, int col, int row);

  /** A lower bound on any value {@link #tileCost} can return. */
  float minTileCost();

  /** Every walkable tile costs 1 — pure shortest-path behaviour. */
  static PathCostStrategy uniform() {
    return new PathCostStrategy() {
      @Override
      public float tileCost(Board board, int col, int row) {
        return 1f;
      }

      @Override
      public float minTileCost() {
        return 1f;
      }
    };
  }
}
