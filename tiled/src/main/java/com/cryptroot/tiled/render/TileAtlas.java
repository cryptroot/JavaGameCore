package com.cryptroot.tiled.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.tiled.io.GlobalTileId;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps global tile ids (gids) to the {@link TextureRegion} that draws them, across every tileset
 * used by a map.
 *
 * <p>An atlas is built once by {@link TiledMapLoader} after the map's tileset images have been
 * loaded and sliced. Lookups are keyed by gid with flip flags already stripped; the empty gid
 * {@code 0} and any unmapped gid return {@code null}.
 *
 * <p>The atlas does <em>not</em> own its textures: they are owned and disposed by the {@code
 * ResourceManager} that loaded them, so an atlas needs no disposal.
 */
public final class TileAtlas {

  /**
   * A drawable tile: the region to sample plus the pixel size at which it should be drawn (a
   * tileset's tiles may be larger than the map's grid cells).
   *
   * @param region the texture region for this tile
   * @param width draw width in world units (the owning tileset's tile width)
   * @param height draw height in world units (the owning tileset's tile height)
   */
  public record Tile(TextureRegion region, float width, float height) {}

  private final Map<Integer, Tile> byGid;

  /**
   * @param byGid mapping of flag-stripped gid to drawable tile; copied defensively
   */
  public TileAtlas(Map<Integer, Tile> byGid) {
    this.byGid = new HashMap<>(byGid);
  }

  /**
   * Looks up the drawable tile for a raw gid.
   *
   * @param rawGid a gid that may still carry flip flags
   * @return the tile, or {@code null} for the empty cell or an unmapped gid
   */
  public Tile tile(int rawGid) {
    int id = GlobalTileId.id(rawGid);
    if (id == 0) {
      return null;
    }
    return byGid.get(id);
  }

  /**
   * @return the number of distinct tiles registered in this atlas.
   */
  public int size() {
    return byGid.size();
  }
}
