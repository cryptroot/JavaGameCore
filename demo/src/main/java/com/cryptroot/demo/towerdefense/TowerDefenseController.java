package com.cryptroot.demo.towerdefense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.cryptroot.core.grid.Grid;
import com.cryptroot.core.path.Board;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldCameraController;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.component.HoverableSpriteComponent;
import com.cryptroot.core.world.component.TextureRenderComponent;
import java.util.Objects;

/**
 * Public entry point wiring the tower-defense mini-game (tower placement, enemy spawning/pathing,
 * and tower-vs-enemy combat) into a {@link World}.
 *
 * <p>Construct one instance per screen/scene, call {@link #tryPlaceTower(float, float)} from a
 * click handler, and let the world pipeline tick the spawner, the placement-preview ghost, and
 * per-entity components as usual.
 */
public final class TowerDefenseController {

  /** Draw size of a placed tower, relative to one grid cell (towers read as slightly oversized). */
  private static final float TOWER_SIZE_SCALE = 1.4f;

  /** Draw size of an enemy, relative to one grid cell. */
  private static final float ENEMY_SIZE_SCALE = 0.8f;

  /** Draw size of a bullet, relative to one grid cell. */
  private static final float BULLET_SIZE_SCALE = 0.4f;

  /** Tower engagement range, in grid cells; also the radius of the hover range indicator. */
  private static final float TOWER_RANGE_CELLS = 4.5f;

  private static final float TOWER_SHOTS_PER_SECOND = 1.5f;
  private static final float BULLET_SPEED_CELLS_PER_SECOND = 8f;
  private static final int BULLET_DAMAGE = 34;

  private static final float ENEMY_SPAWNS_PER_SECOND = 0.5f;
  private static final float ENEMY_SPEED_CELLS_PER_SECOND = 1f;
  private static final int ENEMY_HP = 100;

  private final World world;
  private final PlacementGrid placement;
  private final TextureRegion towerTexture;
  private final TextureRegion bulletTexture;
  private final TextureRegion rangeRingTexture;
  private final float cellSize;

  public TowerDefenseController(
      World world,
      WorldCameraController worldCamera,
      Grid grid,
      Board border,
      TextureRegion towerTexture,
      TextureRegion enemyTexture,
      TextureRegion bulletTexture,
      TextureRegion rangeRingTexture) {
    this.world = Objects.requireNonNull(world, "world must not be null");
    Objects.requireNonNull(worldCamera, "worldCamera must not be null");
    Objects.requireNonNull(grid, "grid must not be null");
    Objects.requireNonNull(border, "border must not be null");
    this.towerTexture = Objects.requireNonNull(towerTexture, "towerTexture must not be null");
    Objects.requireNonNull(enemyTexture, "enemyTexture must not be null");
    this.bulletTexture = Objects.requireNonNull(bulletTexture, "bulletTexture must not be null");
    this.rangeRingTexture =
        Objects.requireNonNull(rangeRingTexture, "rangeRingTexture must not be null");
    this.placement = new PlacementGrid(grid, border);
    this.cellSize = Math.min(grid.cellWidth(), grid.cellHeight());

    EnemySpawnerComponent spawner =
        new EnemySpawnerComponent(
            world,
            placement,
            ENEMY_SPAWNS_PER_SECOND,
            enemyTexture,
            cellSize * ENEMY_SIZE_SCALE,
            cellSize * ENEMY_SPEED_CELLS_PER_SECOND,
            ENEMY_HP);
    world.add(new WorldEntity().with(UpdateComponent.class, spawner));

    float towerSize = cellSize * TOWER_SIZE_SCALE;
    TextureRenderComponent previewRender =
        new TextureRenderComponent(
            towerTexture, 0f, 0f, towerSize, towerSize, RenderPass.FOREGROUND_WORLD);
    previewRender.setVisible(false);
    TowerPlacementPreviewComponent preview =
        new TowerPlacementPreviewComponent(worldCamera, placement, previewRender, towerSize);
    world.add(
        new WorldEntity()
            .with(RenderComponent.class, previewRender)
            .with(UpdateComponent.class, preview));
  }

  /**
   * Attempts to place a tower at the grid cell containing the world-space point {@code (worldX,
   * worldY)}.
   *
   * @return {@code true} if a tower was placed (the cell was not already occupied and would not
   *     seal the enemy lane shut — see {@link PlacementGrid#canPlace(int, int)}); a tower may land
   *     on any cell of the map, floor or border
   */
  public boolean tryPlaceTower(float worldX, float worldY) {
    Grid grid = placement.grid();
    GridPoint2 cell = new GridPoint2();
    if (!grid.worldToCell(worldX, worldY, cell)) return false;
    if (!placement.canPlace(cell.x, cell.y)) return false;

    placement.occupy(cell.x, cell.y);

    var center = grid.cellToWorld(cell.x, cell.y);
    float towerSize = cellSize * TOWER_SIZE_SCALE;
    HoverableSpriteComponent towerSprite =
        new HoverableSpriteComponent(
            towerTexture,
            center.x - towerSize / 2f,
            center.y - towerSize / 2f,
            towerSize,
            towerSize,
            RenderPass.WORLD,
            Color.WHITE);

    float range = cellSize * TOWER_RANGE_CELLS;
    TextureRenderComponent rangeIndicator =
        new TextureRenderComponent(
            rangeRingTexture,
            center.x - range,
            center.y - range,
            range * 2f,
            range * 2f,
            RenderPass.FOREGROUND_WORLD);
    rangeIndicator.setVisible(false);
    towerSprite.onHoverEnter().connect(() -> rangeIndicator.setVisible(true));
    towerSprite.onHoverExit().connect(() -> rangeIndicator.setVisible(false));

    TowerComponent tower =
        new TowerComponent(
            world,
            center.x,
            center.y,
            range,
            TOWER_SHOTS_PER_SECOND,
            bulletTexture,
            cellSize * BULLET_SIZE_SCALE,
            cellSize * BULLET_SPEED_CELLS_PER_SECOND,
            BULLET_DAMAGE);
    world.add(
        new WorldEntity()
            .with(RenderComponent.class, towerSprite)
            .with(TowerComponent.class, tower));
    world.add(new WorldEntity().with(RenderComponent.class, rangeIndicator));
    return true;
  }
}
