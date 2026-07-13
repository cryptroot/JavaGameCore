package com.cryptroot.core.world.component;

import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.time.Motion;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A projectile that homes in on a moving target entity and applies an effect on impact — the
 * framework form of the hand-rolled "bullet chases enemy" logic every projectile-based combat
 * mechanic (a tower, a spell, a homing missile) ends up writing.
 *
 * <p>Every frame, moves its own {@link PositionComponent} toward the target entity's current {@link
 * PositionComponent} via {@link Motion#moveTowards}. On arrival, calls {@code onImpact} once with
 * the target entity and removes itself from {@code world}. If {@code isTargetValid} ever reports
 * the target as no longer usable (dead, despawned, missing its position, …) before arrival, the
 * projectile removes itself <em>without</em> calling {@code onImpact} — this component knows
 * nothing about health, damage, or death, it only asks the predicate supplied by the caller (e.g.
 * {@code e -> e.get(HealthComponent.class).map(HealthComponent::isAlive).orElse(false)}).
 *
 * <p>Must be {@link #bind(WorldEntity)} bound to its own entity before the first {@link
 * #update(float)} — the same pattern used by other self-despawning components in this package.
 */
public final class HomingProjectileComponent implements UpdateComponent {

  private final World world;
  private final PositionComponent position;
  private final WorldEntity target;
  private final float speed;
  private final Predicate<WorldEntity> isTargetValid;
  private final Consumer<WorldEntity> onImpact;
  private final Vector2 scratch = new Vector2();

  private WorldEntity self;
  private boolean done;

  /**
   * @param world used to remove this projectile's entity on impact or a stale target
   * @param position this projectile's own live position, moved every frame
   * @param target the entity being homed in on
   * @param speed movement speed in world units per second; must be positive
   * @param isTargetValid re-checked every frame before {@code target}'s position is trusted; e.g. a
   *     liveness check against the target's own health component
   * @param onImpact invoked exactly once, with {@code target}, when this projectile reaches it
   */
  public HomingProjectileComponent(
      World world,
      PositionComponent position,
      WorldEntity target,
      float speed,
      Predicate<WorldEntity> isTargetValid,
      Consumer<WorldEntity> onImpact) {
    this.world = Objects.requireNonNull(world, "world must not be null");
    this.position = Objects.requireNonNull(position, "position must not be null");
    this.target = Objects.requireNonNull(target, "target must not be null");
    if (speed <= 0f) {
      throw new IllegalArgumentException("speed must be positive: " + speed);
    }
    this.isTargetValid = Objects.requireNonNull(isTargetValid, "isTargetValid must not be null");
    this.onImpact = Objects.requireNonNull(onImpact, "onImpact must not be null");
    this.speed = speed;
  }

  /** Binds the owning entity, required for self-removal on impact or a stale target. */
  public void bind(WorldEntity self) {
    this.self = Objects.requireNonNull(self, "self must not be null");
  }

  @Override
  public void update(float delta) {
    if (done) return;
    if (self == null) {
      throw new IllegalStateException("bind(WorldEntity) must be called before update()");
    }

    Optional<PositionComponent> targetPos = target.get(PositionComponent.class);
    if (targetPos.isEmpty() || !isTargetValid.test(target)) {
      finish(false);
      return;
    }

    scratch.set(position.x(), position.y());
    boolean reached =
        Motion.moveTowards(scratch, targetPos.get().x(), targetPos.get().y(), speed * delta);
    position.moveTo(scratch.x, scratch.y);
    if (reached) {
      finish(true);
    }
  }

  /** {@code true} once this projectile has impacted or given up on a stale target. */
  public boolean isDone() {
    return done;
  }

  private void finish(boolean impacted) {
    done = true;
    if (impacted) onImpact.accept(target);
    world.queueRemove(self);
  }
}
