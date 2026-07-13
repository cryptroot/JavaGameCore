package com.cryptroot.demo.towerdefense;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.grid.Grid;
import com.cryptroot.core.path.PathCostStrategy;
import com.cryptroot.core.path.Pathfinder;
import com.cryptroot.core.physics.BoxCollider;
import com.cryptroot.core.physics.Collider;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.time.Cadence;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.component.HealthComponent;
import com.cryptroot.core.world.component.PathFollowerComponent;
import com.cryptroot.core.world.component.TextureRenderComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Game-specific enemy spawner: periodically drops an enemy at a random floor column on the bottom
 * row of the arena (never the dark-brown/black border — see {@link PlacementGrid}), pathfinding
 * around any placed towers to the nearest open floor column on the top row.
 *
 * <p>Each enemy also carries a {@link BoxCollider} matching its sprite bounds, so {@link
 * com.cryptroot.core.physics.CollisionSystem} can detect a homing bullet touching it — see {@link
 * TowerComponent}.
 */
final class EnemySpawnerComponent implements UpdateComponent {

  private final World world;
  private final PlacementGrid placement;
  private final Cadence cadence;
  private final TextureRegion enemyTexture;
  private final float enemySize;
  private final float enemySpeed;
  private final int enemyHp;

  EnemySpawnerComponent(
      World world,
      PlacementGrid placement,
      float spawnsPerSecond,
      TextureRegion enemyTexture,
      float enemySize,
      float enemySpeed,
      int enemyHp) {
    this.world = Objects.requireNonNull(world, "world must not be null");
    this.placement = Objects.requireNonNull(placement, "placement must not be null");
    this.enemyTexture = Objects.requireNonNull(enemyTexture, "enemyTexture must not be null");
    if (enemySize <= 0f) {
      throw new IllegalArgumentException("enemySize must be positive: " + enemySize);
    }
    if (enemySpeed <= 0f) {
      throw new IllegalArgumentException("enemySpeed must be positive: " + enemySpeed);
    }
    if (enemyHp <= 0) {
      throw new IllegalArgumentException("enemyHp must be positive: " + enemyHp);
    }
    this.cadence = new Cadence(1f / spawnsPerSecond);
    this.enemySize = enemySize;
    this.enemySpeed = enemySpeed;
    this.enemyHp = enemyHp;
  }

  @Override
  public void update(float delta) {
    cadence.update(delta);
    if (cadence.consumeReady()) spawn();
  }

  private void spawn() {
    Grid grid = placement.grid();
    int preferredCol = MathUtils.random(placement.minSpawnColumn(), placement.maxSpawnColumn());
    GridPoint2 start = placement.nearestOpenInRow(0, preferredCol).orElse(null);
    if (start == null) return; // bottom row fully occupied — skip this spawn

    GridPoint2 goal = placement.nearestOpenInRow(grid.rows() - 1, start.x).orElse(null);
    if (goal == null) return; // top row fully occupied — skip this spawn

    List<GridPoint2> path =
        Pathfinder.findPath(grid, start, goal, placement.board(), PathCostStrategy.uniform());
    if (path.isEmpty()) return; // no route around current towers — skip this spawn

    List<Vector2> waypoints = new ArrayList<>(path.size());
    for (GridPoint2 cell : path) waypoints.add(grid.cellToWorld(cell.x, cell.y));

    Vector2 spawnPos = waypoints.get(0);
    TextureRenderComponent render =
        new TextureRenderComponent(
            enemyTexture,
            spawnPos.x - enemySize / 2f,
            spawnPos.y - enemySize / 2f,
            enemySize,
            enemySize,
            RenderPass.WORLD);
    HealthComponent health = new HealthComponent(enemyHp);
    PathFollowerComponent pathFollower = new PathFollowerComponent(render, waypoints, enemySpeed);
    BoxCollider collider = new BoxCollider(render, 0f, 0f, enemySize, enemySize);

    // Spawns from inside this component's own update() while UpdateSystem iterates the live
    // entity list, so this must go through queueAdd rather than World.add.
    WorldEntity enemy =
        world.queueAdd(
            new WorldEntity()
                .with(RenderComponent.class, render)
                .with(HealthComponent.class, health)
                .with(UpdateComponent.class, pathFollower)
                .with(Collider.class, collider));
    // Reaching the final waypoint (leaked through) and dying to damage both despawn the enemy;
    // World.queueRemove collapses duplicate requests, so no extra guard is needed if both fire.
    pathFollower.onCompleted().connect(() -> world.queueRemove(enemy));
    health.onDeath().connect(() -> world.queueRemove(enemy));
  }
}
