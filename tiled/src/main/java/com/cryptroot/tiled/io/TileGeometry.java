package com.cryptroot.tiled.io;

import com.badlogic.gdx.math.GridPoint2;

/**
 * Pure coordinate math converting orthogonal tile grid positions into libGDX world coordinates.
 *
 * <p>Tiled places its origin at the top-left with the y-axis pointing down, and addresses tiles by
 * {@code (column, row)} where row {@code 0} is the top row. libGDX uses a bottom-left origin with
 * the y-axis pointing up. These helpers map a tile's grid position to the world coordinates of the
 * tile quad's bottom-left corner, flipping the y-axis so that row {@code 0} ends up at the top of
 * the map.
 *
 * <p>Layer pixel offsets are not applied here; callers add them after the fact.
 */
public final class TileGeometry {

  private TileGeometry() {}

  /**
   * @param column zero-based column index (increasing rightward)
   * @param tileWidth tile width in pixels
   * @return the world x of the tile quad's left edge
   */
  public static float worldX(int column, int tileWidth) {
    return column * (float) tileWidth;
  }

  /**
   * @param row zero-based row index (increasing downward, row {@code 0} is the top)
   * @param mapHeight map height in tiles
   * @param tileHeight tile height in pixels
   * @return the world y of the tile quad's bottom edge, with the y-axis flipped so the top row sits
   *     at the top of the map
   */
  public static float worldY(int row, int mapHeight, int tileHeight) {
    return (mapHeight - 1 - row) * (float) tileHeight;
  }

  /**
   * @param column zero-based column index
   * @param row zero-based row index
   * @param width map width in tiles
   * @return the index of the cell within a row-major gid array
   */
  public static int index(int column, int row, int width) {
    return row * width + column;
  }

  // -------------------------------------------------------------------------
  // Inverse: world -> tile grid (matches the forward y-flip above)
  // -------------------------------------------------------------------------

  /**
   * @param worldX world x in pixels
   * @param tileWidth tile width in pixels
   * @return the column containing {@code worldX} (may be out of range; callers validate against the
   *     map width)
   */
  public static int columnAt(float worldX, int tileWidth) {
    return (int) Math.floor(worldX / (float) tileWidth);
  }

  /**
   * Inverse of {@link #worldY}: the Tiled row (row {@code 0} = top) containing {@code worldY}, with
   * the y-axis flipped back.
   *
   * @param worldY world y in pixels
   * @param mapHeight map height in tiles
   * @param tileHeight tile height in pixels
   */
  public static int rowAt(float worldY, int mapHeight, int tileHeight) {
    return (mapHeight - 1) - (int) Math.floor(worldY / (float) tileHeight);
  }

  /**
   * Writes the tile {@code (column,row)} containing world point {@code (worldX,worldY)} into {@code
   * out} and returns {@code true}; returns {@code false} and leaves {@code out} unchanged when the
   * point falls outside the {@code mapWidth × mapHeight} tile grid.
   */
  public static boolean cellAt(
      float worldX,
      float worldY,
      int mapWidth,
      int mapHeight,
      int tileWidth,
      int tileHeight,
      GridPoint2 out) {
    int col = columnAt(worldX, tileWidth);
    int row = rowAt(worldY, mapHeight, tileHeight);
    if (col < 0 || col >= mapWidth || row < 0 || row >= mapHeight) return false;
    out.set(col, row);
    return true;
  }
}
