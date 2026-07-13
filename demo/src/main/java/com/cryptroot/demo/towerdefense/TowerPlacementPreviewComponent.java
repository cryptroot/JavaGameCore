package com.cryptroot.demo.towerdefense;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.cryptroot.core.grid.Grid;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.WorldCameraController;
import com.cryptroot.core.world.component.TextureRenderComponent;
import java.util.Objects;

/**
 * Game-specific placement ghost: every frame, snaps to the grid cell under the cursor and shows a
 * translucent tower sprite there so the player can see exactly where a click will land before
 * committing — tinted white when that cell would accept a tower and red when it would not (see
 * {@link PlacementGrid#canPlace(int, int)}). Hidden entirely while the cursor is off the grid.
 *
 * <p>The validity check is only re-run when the hovered cell actually changes (not every frame) —
 * {@link PlacementGrid#canPlace} does a small flood-fill, cheap per call but wasteful to repeat 60
 * times a second while the mouse sits still.
 */
final class TowerPlacementPreviewComponent implements UpdateComponent {

  private static final Color VALID_TINT = new Color(1f, 1f, 1f, 0.55f);
  private static final Color INVALID_TINT = new Color(0.95f, 0.3f, 0.3f, 0.55f);

  private final WorldCameraController worldCamera;
  private final PlacementGrid placement;
  private final TextureRenderComponent render;
  private final float size;
  private final GridPoint2 cellScratch = new GridPoint2();
  private final Vector2 centerScratch = new Vector2();

  private boolean hasLastCell;
  private int lastCol;
  private int lastRow;

  TowerPlacementPreviewComponent(
      WorldCameraController worldCamera,
      PlacementGrid placement,
      TextureRenderComponent render,
      float size) {
    this.worldCamera = Objects.requireNonNull(worldCamera, "worldCamera must not be null");
    this.placement = Objects.requireNonNull(placement, "placement must not be null");
    this.render = Objects.requireNonNull(render, "render must not be null");
    if (size <= 0f) {
      throw new IllegalArgumentException("size must be positive: " + size);
    }
    this.size = size;
  }

  @Override
  public void update(float delta) {
    Grid grid = placement.grid();
    Vector3 mouse = worldCamera.unproject(Gdx.input.getX(), Gdx.input.getY());
    if (!grid.worldToCell(mouse.x, mouse.y, cellScratch)) {
      render.setVisible(false);
      hasLastCell = false;
      return;
    }

    grid.cellToWorld(cellScratch.x, cellScratch.y, centerScratch);
    render.setVisible(true);
    render.moveTo(centerScratch.x - size / 2f, centerScratch.y - size / 2f);

    if (!hasLastCell || cellScratch.x != lastCol || cellScratch.y != lastRow) {
      hasLastCell = true;
      lastCol = cellScratch.x;
      lastRow = cellScratch.y;
      render.setTint(placement.canPlace(lastCol, lastRow) ? VALID_TINT : INVALID_TINT);
    }
  }
}
