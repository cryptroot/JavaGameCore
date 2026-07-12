package com.cryptroot.demo.towerdefense;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.time.Cadence;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.component.TextureRenderComponent;
import java.util.Objects;
import java.util.Optional;

/**
 * Game-specific tower behaviour: periodically fires a homing bullet at the nearest enemy within
 * range.
 */
final class TowerComponent implements UpdateComponent {

  private final World world;
  private final float x;
  private final float y;
  private final float range;
  private final Cadence cadence;
  private final TextureRegion bulletTexture;
  private final float bulletSize;
  private final float bulletSpeed;
  private final int damage;

  TowerComponent(
      World world,
      float x,
      float y,
      float range,
      float shotsPerSecond,
      TextureRegion bulletTexture,
      float bulletSize,
      float bulletSpeed,
      int damage) {
    this.world = Objects.requireNonNull(world, "world must not be null");
    this.bulletTexture = Objects.requireNonNull(bulletTexture, "bulletTexture must not be null");
    if (range <= 0f) {
      throw new IllegalArgumentException("range must be positive: " + range);
    }
    if (bulletSize <= 0f) {
      throw new IllegalArgumentException("bulletSize must be positive: " + bulletSize);
    }
    if (bulletSpeed <= 0f) {
      throw new IllegalArgumentException("bulletSpeed must be positive: " + bulletSpeed);
    }
    if (damage <= 0) {
      throw new IllegalArgumentException("damage must be positive: " + damage);
    }
    this.x = x;
    this.y = y;
    this.range = range;
    this.cadence = new Cadence(1f / shotsPerSecond);
    this.bulletSize = bulletSize;
    this.bulletSpeed = bulletSpeed;
    this.damage = damage;
  }

  @Override
  public void update(float delta) {
    cadence.update(delta);
    if (!cadence.consumeReady()) return;

    findNearestEnemyInRange().ifPresent(this::fireAt);
  }

  private Optional<WorldEntity> findNearestEnemyInRange() {
    WorldEntity nearest = null;
    float nearestDistSq = range * range;
    for (WorldEntity e : world.entities()) {
      Optional<EnemyComponent> enemy = e.get(EnemyComponent.class);
      if (enemy.isEmpty() || !enemy.get().isAlive()) continue;
      PositionComponent pos = e.get(PositionComponent.class).orElse(null);
      if (pos == null) continue;
      float dx = pos.x() - x;
      float dy = pos.y() - y;
      float distSq = dx * dx + dy * dy;
      if (distSq <= nearestDistSq) {
        nearest = e;
        nearestDistSq = distSq;
      }
    }
    return Optional.ofNullable(nearest);
  }

  private void fireAt(WorldEntity target) {
    TextureRenderComponent bulletRender =
        new TextureRenderComponent(
            bulletTexture,
            x - bulletSize / 2f,
            y - bulletSize / 2f,
            bulletSize,
            bulletSize,
            RenderPass.WORLD);
    BulletComponent bulletComponent =
        new BulletComponent(world, bulletRender, target, bulletSpeed, damage);
    // Fires from inside this component's own update() while UpdateSystem iterates the live entity
    // list, so this must go through queueAdd rather than World.add.
    WorldEntity bullet =
        world.queueAdd(
            new WorldEntity()
                .with(RenderComponent.class, bulletRender)
                .with(BulletComponent.class, bulletComponent));
    bulletComponent.bind(bullet);
  }
}
