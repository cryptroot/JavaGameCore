package com.cryptroot.tiled.io;

/**
 * A rectangular sub-region of a tileset image, in image pixel coordinates with a top-left origin
 * (the convention libGDX's {@code TextureRegion} uses).
 *
 * @param x left edge in pixels
 * @param y top edge in pixels
 * @param width width in pixels
 * @param height height in pixels
 */
public record TileRect(int x, int y, int width, int height) {
  public TileRect {
    if (width < 0) {
      throw new IllegalArgumentException("width must not be negative, was " + width);
    }
    if (height < 0) {
      throw new IllegalArgumentException("height must not be negative, was " + height);
    }
  }
}
