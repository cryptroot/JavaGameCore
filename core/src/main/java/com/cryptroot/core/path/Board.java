package com.cryptroot.core.path;

/**
 * A read-only, framework-neutral view of which cells may not be entered.
 *
 * <p>The {@link Pathfinder} knows nothing about game concepts — it asks only whether a cell is
 * blocked. A game folds everything that makes a cell impassable (out-of-lane, occupied by a hard
 * obstacle, off a walkable band, …) into a single {@link #isBlocked} answer. Traversal
 * <em>cost</em> — as opposed to hard passability — is expressed separately through a {@link
 * PathCostStrategy}.
 */
@FunctionalInterface
public interface Board {

  /** {@code true} if cell {@code (col,row)} cannot be entered at all. */
  boolean isBlocked(int col, int row);

  /** A board where no cell is ever blocked. */
  static Board open() {
    return (col, row) -> false;
  }
}
