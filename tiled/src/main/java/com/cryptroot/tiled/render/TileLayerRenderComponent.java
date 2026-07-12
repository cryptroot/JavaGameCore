package com.cryptroot.tiled.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.tiled.io.GlobalTileId;
import com.cryptroot.tiled.io.TileGeometry;
import com.cryptroot.tiled.model.TileLayer;
import java.util.Objects;

/**
 * Draws a single orthogonal {@link TileLayer} through the core render pipeline.
 *
 * <p>One component draws an entire layer in a single {@link #draw} call — mirroring {@code
 * PolygonTileRenderComponent} — so a map becomes one entity per tile layer. Layers are drawn in the
 * {@link RenderPass} chosen at construction (defaulting to {@link RenderPass#BACKGROUND}, which the
 * core pipeline draws in insertion order, so adding layers in document order preserves their draw
 * order).
 *
 * <p>Empty cells are skipped. Horizontal and vertical tile flips are honoured; anti-diagonal
 * (rotational) flips are not yet applied. Layer opacity and an optional tint colour are applied by
 * tinting the batch for the duration of the layer.
 */
public final class TileLayerRenderComponent implements RenderComponent {

  private final int[] gids;
  private final int mapWidth;
  private final int mapHeight;
  private final int mapTileWidth;
  private final int mapTileHeight;
  private final float offsetX;
  private final float offsetY;
  private final float opacity;
  private final Color tint;
  private final TileAtlas atlas;
  private final RenderPass renderPass;

  /**
   * @param layer the source tile layer (for name, offset, opacity, tint)
   * @param gids decoded raw gids for the layer, row-major, length {@code mapWidth*mapHeight}
   * @param mapWidth map width in tiles
   * @param mapHeight map height in tiles
   * @param mapTileWidth grid cell width in pixels
   * @param mapTileHeight grid cell height in pixels
   * @param atlas gid-to-region atlas for the map
   * @param renderPass the pass this layer is drawn in
   */
  public TileLayerRenderComponent(
      TileLayer layer,
      int[] gids,
      int mapWidth,
      int mapHeight,
      int mapTileWidth,
      int mapTileHeight,
      TileAtlas atlas,
      RenderPass renderPass) {
    Objects.requireNonNull(layer, "layer must not be null");
    Objects.requireNonNull(gids, "gids must not be null");
    Objects.requireNonNull(atlas, "atlas must not be null");
    Objects.requireNonNull(renderPass, "renderPass must not be null");
    if (mapWidth <= 0 || mapHeight <= 0) {
      throw new IllegalArgumentException("mapWidth and mapHeight must be positive");
    }
    if (mapTileWidth <= 0 || mapTileHeight <= 0) {
      throw new IllegalArgumentException("mapTileWidth and mapTileHeight must be positive");
    }
    this.gids = gids;
    this.mapWidth = mapWidth;
    this.mapHeight = mapHeight;
    this.mapTileWidth = mapTileWidth;
    this.mapTileHeight = mapTileHeight;
    this.offsetX = layer.offsetX();
    this.offsetY = layer.offsetY();
    this.opacity = layer.opacity();
    this.tint = layer.tintColor() != null ? TmxColors.parse(layer.tintColor()) : null;
    this.atlas = atlas;
    this.renderPass = renderPass;
  }

  @Override
  public void draw(PolygonSpriteBatch batch) {
    boolean tinted = tint != null || opacity < 1f;
    if (tinted) {
      float r = tint != null ? tint.r : 1f;
      float g = tint != null ? tint.g : 1f;
      float b = tint != null ? tint.b : 1f;
      float a = (tint != null ? tint.a : 1f) * opacity;
      batch.setColor(r, g, b, a);
    }

    for (int row = 0; row < mapHeight; row++) {
      for (int col = 0; col < mapWidth; col++) {
        int raw = gids[TileGeometry.index(col, row, mapWidth)];
        TileAtlas.Tile t = atlas.tile(raw);
        if (t == null) {
          continue;
        }
        float x = TileGeometry.worldX(col, mapTileWidth) + offsetX;
        float y = TileGeometry.worldY(row, mapHeight, mapTileHeight) - offsetY;

        TextureRegion region = t.region();
        boolean flipH = GlobalTileId.isFlippedHorizontally(raw);
        boolean flipV = GlobalTileId.isFlippedVertically(raw);
        if (flipH || flipV) {
          region.flip(flipH, flipV);
          batch.draw(region, x, y, t.width(), t.height());
          region.flip(flipH, flipV);
        } else {
          batch.draw(region, x, y, t.width(), t.height());
        }
      }
    }

    if (tinted) {
      batch.setColor(Color.WHITE);
    }
  }

  @Override
  public RenderPass renderPass() {
    return renderPass;
  }

  /** Tile layers are not Y-sorted; drawing order is the pass's insertion order. */
  @Override
  public float sortKey() {
    return 0f;
  }
}
