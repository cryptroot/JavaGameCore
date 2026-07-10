package com.cryptroot.tiled.render;

import com.cryptroot.core.path.Board;
import com.cryptroot.tiled.io.GlobalTileId;
import com.cryptroot.tiled.io.TileDataCodec;
import com.cryptroot.tiled.io.TileGeometry;
import com.cryptroot.tiled.model.TileLayer;
import com.cryptroot.tiled.model.TmxMap;
import java.util.function.IntPredicate;

/**
 * Bridges a decoded {@link TileLayer} into a {@link Board} for collision/pathfinding blocking.
 *
 * <p>Decodes the layer's gids once, then answers {@link Board#isBlocked(int, int)} queries in
 * {@code core.grid}'s bottom-up row convention (row 0 = world bottom) by flipping back to Tiled's
 * top-down row order — the same flip {@link TiledGrids#fromMap} documents — and stripping any
 * flip-flag bits via {@link GlobalTileId#id(int)} before testing the caller-supplied predicate.
 *
 * <p>Deliberately game-agnostic about <em>which</em> gid(s) block movement — that is supplied as
 * {@code blockedGid}, so the same mechanical gid-array/row-flip logic is reusable across any game
 * built on this framework, while "what counts as unwalkable" stays entirely game-specific.
 */
public final class TiledBoards {

  private TiledBoards() {}

  /**
   * @param map the owning map (supplies width/height for the row-flip and gid-array indexing)
   * @param layer the tile layer whose gids determine blocking; must belong to {@code map}
   * @param blockedGid tested against each cell's flip-flag-stripped gid; {@code true} blocks it
   * @return a {@link Board} that answers queries for any {@code (col,row)} within {@code
   *     map}'s bounds ({@code core.grid} convention: row 0 = bottom)
   */
  public static Board fromLayer(TmxMap map, TileLayer layer, IntPredicate blockedGid) {
    int[] gids = TileDataCodec.decode(layer.data(), layer.width() * layer.height());
    int mapWidth = map.width();
    int mapHeight = map.height();

    return (col, row) -> {
      int tiledRow = mapHeight - 1 - row;
      int gid = GlobalTileId.id(gids[TileGeometry.index(col, tiledRow, mapWidth)]);
      return blockedGid.test(gid);
    };
  }
}
