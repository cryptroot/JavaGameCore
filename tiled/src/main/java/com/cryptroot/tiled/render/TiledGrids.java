package com.cryptroot.tiled.render;

import com.cryptroot.core.grid.Grid;
import com.cryptroot.tiled.model.TmxMap;
import java.util.Objects;

/**
 * Optional convenience for deriving a {@link Grid} from a parsed {@link TmxMap}.
 *
 * <p>Most games define their logical grid independently of the map (the map is often just a visual
 * backdrop, and a coarse gameplay grid may span several fine tiles). Use this only when the
 * gameplay grid <em>is</em> the tile grid.
 *
 * <p>The derived grid uses world-unit == pixel scaling: cell size equals tile size and dimensions
 * equal the map's tile dimensions. Grid coordinates follow {@code core}'s bottom-left/y-up
 * convention and align with {@link com.cryptroot.tiled.io.TileGeometry} world coordinates — so grid
 * row {@code 0} is the world <em>bottom</em> (Tiled's bottom row), not Tiled's top row {@code 0}.
 */
public final class TiledGrids {

  private TiledGrids() {}

  /** A {@link Grid} at world origin {@code (0,0)} matching the map's tile dimensions. */
  public static Grid fromMap(TmxMap map) {
    return fromMap(map, 0f, 0f);
  }

  /**
   * A {@link Grid} whose bottom-left corner of cell {@code (0,0)} sits at {@code
   * (originX,originY)}, with one cell per tile.
   */
  public static Grid fromMap(TmxMap map, float originX, float originY) {
    Objects.requireNonNull(map, "map must not be null");
    return new Grid(originX, originY, map.tileWidth(), map.tileHeight(), map.width(), map.height());
  }
}
