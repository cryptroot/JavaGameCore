package com.cryptroot.core.world.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.RenderComponent;

/**
 * Renders an isometric diamond tile grid that scrolls with the world camera.
 *
 * <p>Each frame the camera position is snapped to the nearest tile-grid point so tiles occupy fixed
 * world-space positions. The camera's own projection matrix then scrolls them naturally — panning
 * the camera makes the floor scroll in the opposite direction while world-layer entities stay in
 * place. Tiles alternate between two colours in a checkerboard pattern.
 *
 * <p>This component belongs to the {@link RenderLayer#BACKGROUND} layer and is drawn before any
 * WORLD-layer entities. It has no {@code BoundsComponent} — the floor is never individually
 * hit-tested or clickable.
 *
 * <h3>Isometric tile mapping</h3>
 *
 * The tile at absolute grid index {@code (absTx, absTy)} has world position:
 *
 * <pre>
 *   wx = (absTx - absTy) * tileW / 2
 *   wy = (absTx + absTy) * tileH / 2
 * </pre>
 */
public final class PolygonTileRenderComponent implements RenderComponent {

  private final PolygonRegion tile;
  private final Color colorA;
  private final Color colorB;
  private final float tileW;
  private final float tileH;
  private final int gridHalf;
  private final OrthographicCamera camera;

  /**
   * @param pixel a 1×1 white pixel texture (from {@code MyJourneyGame.getPixel()})
   * @param colorA checkerboard colour A (dark tile)
   * @param colorB checkerboard colour B (light tile)
   * @param tileW diamond width in world units (x-axis extent)
   * @param tileH diamond height in world units (y-axis extent)
   * @param gridHalf half the grid count per axis; total grid = {@code (2*gridHalf+1)²} tiles
   * @param camera world camera whose position drives the grid centre each frame
   */
  public PolygonTileRenderComponent(
      Texture pixel,
      Color colorA,
      Color colorB,
      float tileW,
      float tileH,
      int gridHalf,
      OrthographicCamera camera) {
    this.colorA = new Color(colorA);
    this.colorB = new Color(colorB);
    this.tileW = tileW;
    this.tileH = tileH;
    this.gridHalf = gridHalf;
    this.camera = camera;

    float hw = tileW / 2f;
    float hh = tileH / 2f;
    // Diamond vertices centred at local origin so batch.draw(tile, cx, cy) places
    // the tile centre at (cx, cy).
    tile =
        new PolygonRegion(
            new TextureRegion(pixel),
            new float[] {0, hh, hw, 0, 0, -hh, -hw, 0},
            new short[] {0, 1, 3, 1, 2, 3});
  }

  // -------------------------------------------------------------------------
  // RenderComponent
  // -------------------------------------------------------------------------

  @Override
  public void draw(PolygonSpriteBatch batch) {
    float halfTileW = tileW / 2f;
    float halfTileH = tileH / 2f;

    // Snap the camera position to the nearest tile-grid point.
    // Inverse iso: absTx = camX/tileW + camY/tileH
    //              absTy = camY/tileH - camX/tileW
    float camX = camera.position.x;
    float camY = camera.position.y;
    int snapTx = Math.round(camX / tileW + camY / tileH);
    int snapTy = Math.round(camY / tileH - camX / tileW);

    // World position of the snapped tile centre.
    float originX = (snapTx - snapTy) * halfTileW;
    float originY = (snapTx + snapTy) * halfTileH;

    // Expand the grid when zoomed out so tiles always fill the visible area.
    // Each extra zoom unit roughly doubles the world area, so scale gridHalf by zoom
    // and add a 2-tile safety margin to avoid edge pop-in during dragging.
    int dynGridHalf = (int) Math.ceil(gridHalf * Math.max(1f, camera.zoom)) + 2;

    for (int tx = -dynGridHalf; tx <= dynGridHalf; tx++) {
      for (int ty = -dynGridHalf; ty <= dynGridHalf; ty++) {
        float cx = originX + (tx - ty) * halfTileW;
        float cy = originY + (tx + ty) * halfTileH;
        // Use absolute tile indices for a consistent checkerboard pattern.
        int absTx = snapTx + tx;
        int absTy = snapTy + ty;
        batch.setColor(((absTx + absTy) & 1) == 0 ? colorA : colorB);
        batch.draw(tile, cx, cy);
      }
    }
    batch.setColor(Color.WHITE);
  }

  @Override
  public RenderPass renderPass() {
    return RenderPass.BACKGROUND;
  }

  /** Not used for BACKGROUND entities; returns 0. */
  @Override
  public float sortKey() {
    return 0f;
  }
}
