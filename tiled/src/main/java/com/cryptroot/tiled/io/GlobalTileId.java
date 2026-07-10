package com.cryptroot.tiled.io;

/**
 * Helpers for interpreting <a
 * href="https://doc.mapeditor.org/en/stable/reference/global-tile-ids/">global tile ids</a> (gids).
 *
 * <p>A gid stored in tile-layer data or on a tile object packs three flip flags into its high bits
 * alongside the actual tile id in the low 29 bits. The gids are treated as raw 32-bit values;
 * because Java has no unsigned {@code int}, a gid with the horizontal-flip bit set appears as a
 * negative {@code int}, which is harmless as long as the values are only ever inspected through
 * these helpers.
 */
public final class GlobalTileId {

  /** Bit set when the tile is flipped horizontally. */
  public static final int FLIP_HORIZONTAL = 0x80000000;

  /** Bit set when the tile is flipped vertically. */
  public static final int FLIP_VERTICAL = 0x40000000;

  /** Bit set when the tile is flipped along its anti-diagonal (a 90° rotation component). */
  public static final int FLIP_DIAGONAL = 0x20000000;

  private static final int FLAG_MASK = FLIP_HORIZONTAL | FLIP_VERTICAL | FLIP_DIAGONAL;

  private GlobalTileId() {}

  /**
   * @param rawGid a raw global tile id
   * @return the tile id with all flip flags stripped (the low 29 bits)
   */
  public static int id(int rawGid) {
    return rawGid & ~FLAG_MASK;
  }

  /**
   * @return {@code true} when the horizontal-flip flag is set on {@code rawGid}.
   */
  public static boolean isFlippedHorizontally(int rawGid) {
    return (rawGid & FLIP_HORIZONTAL) != 0;
  }

  /**
   * @return {@code true} when the vertical-flip flag is set on {@code rawGid}.
   */
  public static boolean isFlippedVertically(int rawGid) {
    return (rawGid & FLIP_VERTICAL) != 0;
  }

  /**
   * @return {@code true} when the anti-diagonal-flip flag is set on {@code rawGid}.
   */
  public static boolean isFlippedDiagonally(int rawGid) {
    return (rawGid & FLIP_DIAGONAL) != 0;
  }

  /**
   * @return {@code true} when {@code rawGid} refers to no tile (the empty cell).
   */
  public static boolean isEmpty(int rawGid) {
    return id(rawGid) == 0;
  }
}
