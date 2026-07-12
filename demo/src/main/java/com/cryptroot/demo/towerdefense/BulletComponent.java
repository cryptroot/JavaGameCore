package com.cryptroot.demo.towerdefense;

import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.time.Motion;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import java.util.Objects;

/**
 * Game-specific projectile behaviour: homes in on a target enemy entity and applies damage on
 * impact. Self-destructs (without dealing damage) if the target died before impact.
 */
final class BulletComponent implements UpdateComponent {

  private final World world;
  private final PositionComponent position;
  private final WorldEntity target;
  private final float speed;
  private final int damage;
  private final Vector2 scratch = new Vector2();

  private WorldEntity self;

  BulletComponent(
      World world, PositionComponent position, WorldEntity target, float speed, int damage) {
    this.world = Objects.requireNonNull(world, "world must not be null");
    this.position = Objects.requireNonNull(position, "position must not be null");
    this.target = Objects.requireNonNull(target, "target must not be null");
    if (speed <= 0f) {
      throw new IllegalArgumentException("speed must be positive: " + speed);
    }
    if (damage <= 0) {
      throw new IllegalArgumentException("damage must be positive: " + damage);
    }
    this.speed = speed;
    this.damage = damage;
  }

  /** Binds the owning entity, required for self-removal on impact or a stale target. */
  void bind(WorldEntity self) {
    this.self = Objects.requireNonNull(self, "self must not be null");
  }

  @Override
  public void update(float delta) {
    EnemyComponent enemy = target.get(EnemyComponent.class).orElse(null);
    PositionComponent targetPos = target.get(PositionComponent.class).orElse(null);
    if (enemy == null || targetPos == null || !enemy.isAlive()) {
      world.queueRemove(self);
      return;
    }

    scratch.set(position.x(), position.y());
    boolean reached = Motion.moveTowards(scratch, targetPos.x(), targetPos.y(), speed * delta);
    position.moveTo(scratch.x, scratch.y);
    if (reached) {
      enemy.damage(damage);
      world.queueRemove(self);
    }
  }
}
