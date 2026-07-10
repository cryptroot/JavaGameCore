package com.cryptroot.tiled.io;

/**
 * Pure math that locates a tile within a single-image tileset.
 *
 * <p>Tiles are laid out left-to-right, top-to-bottom in a grid of {@code columns} columns,
 * separated by {@code spacing} pixels and inset from the image edges by {@code margin} pixels.
 */
public final class TileSliceMath {

  private TileSliceMath() {}

  /**
   * Computes the pixel rectangle of a tile within its tileset image.
   *
   * @param localId zero-based tile index within the tileset
   * @param columns number of tile columns in the image
   * @param tileWidth tile width in pixels
   * @param tileHeight tile height in pixels
   * @param margin margin in pixels around the tiles
   * @param spacing spacing in pixels between adjacent tiles
   * @return the tile's rectangle in image pixel coordinates (top-left origin)
   */
  public static TileRect rect(
      int localId, int columns, int tileWidth, int tileHeight, int margin, int spacing) {
    if (columns <= 0) {
      throw new IllegalArgumentException("columns must be positive, was " + columns);
    }
    int col = localId % columns;
    int row = localId / columns;
    int x = margin + col * (tileWidth + spacing);
    int y = margin + row * (tileHeight + spacing);
    return new TileRect(x, y, tileWidth, tileHeight);
  }
}
