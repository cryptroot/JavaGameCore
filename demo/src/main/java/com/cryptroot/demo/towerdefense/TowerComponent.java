package com.cryptroot.demo.towerdefense;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.core.event.Signal;
import com.cryptroot.core.physics.BoxCollider;
import com.cryptroot.core.physics.Collider;
import com.cryptroot.core.physics.CollisionListener;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.time.Cadence;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.WorldQueries;
import com.cryptroot.core.world.component.HealthComponent;
import com.cryptroot.core.world.component.HomingProjectileComponent;
import com.cryptroot.core.world.component.TextureRenderComponent;
import java.util.Objects;

/**
 * Game-specific tower behaviour: periodically fires a homing bullet at the nearest enemy within
 * range.
 *
 * <p>Impact is confirmed by shape overlap — both the bullet and its target enemy carry a {@link
 * BoxCollider}, and {@link com.cryptroot.core.physics.CollisionSystem} (wired unconditionally into
 * every {@link com.cryptroot.core.screen.BaseGameScreen}) reports the overlap to the bullet's
 * {@link BulletImpactListener} — rather than the bullet reaching the enemy's exact position. {@link
 * HomingProjectileComponent}'s own arrival callback is a deliberate no-op here; see {@link
 * #fireAt(WorldEntity)}.
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

    WorldQueries.nearest(world, x, y, range, HealthComponent.class, HealthComponent::isAlive)
        .ifPresent(this::fireAt);
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
    BoxCollider bulletCollider = new BoxCollider(bulletRender, 0f, 0f, bulletSize, bulletSize);
    HomingProjectileComponent projectile =
        new HomingProjectileComponent(
            world,
            bulletRender,
            target,
            bulletSpeed,
            e -> e.get(HealthComponent.class).map(HealthComponent::isAlive).orElse(false),
            // Damage is applied by BulletImpactListener on collider overlap instead (see below), so
            // this is a deliberate no-op — applying it here too would double-damage the target on
            // the (common) frame where the bullet both overlaps and arrives at the exact position.
            e -> {});

    WorldEntity bullet =
        new WorldEntity()
            .with(RenderComponent.class, bulletRender)
            .with(UpdateComponent.class, projectile)
            .with(Collider.class, bulletCollider);
    bullet.with(CollisionListener.class, new BulletImpactListener(world, bullet, target, damage));
    // Fires from inside this component's own update() while UpdateSystem iterates the live entity
    // list, so this must go through queueAdd rather than World.add.
    world.queueAdd(bullet);
    projectile.bind(bullet);
  }

  /**
   * Applies damage and despawns the bullet the instant its {@link BoxCollider} overlaps its
   * intended target's — ignoring incidental overlaps with anything else (other enemies, other
   * bullets). {@link HealthComponent#damage} is itself a no-op once the target is already dead, so
   * multiple bullets homing on the same enemy cannot over-apply damage even if their overlaps are
   * detected in the same frame.
   */
  private static final class BulletImpactListener implements CollisionListener {
    private final Signal<WorldEntity> onCollisionEnter = new Signal<>();
    private final Signal<WorldEntity> onCollisionExit = new Signal<>();

    BulletImpactListener(World world, WorldEntity bullet, WorldEntity target, int damage) {
      onCollisionEnter.connect(
          other -> {
            if (other != target) return;
            other.get(HealthComponent.class).ifPresent(h -> h.damage(damage));
            world.queueRemove(bullet);
          });
    }

    @Override
    public Signal<WorldEntity> onCollisionEnter() {
      return onCollisionEnter;
    }

    @Override
    public Signal<WorldEntity> onCollisionExit() {
      return onCollisionExit;
    }
  }
}
