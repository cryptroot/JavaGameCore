package com.cryptroot.core.physics;

import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.world.EntityComponent;
import java.util.Objects;

/**
 * A 2D collision shape.
 *
 * <p>This is deliberately minimal so many different concrete shapes can implement it (an AABB
 * today; a circle, a polygon, or a compound shape later) without a heavyweight double-dispatch
 * hierarchy. Every collider must be able to report a conservative axis-aligned bounding box via
 * {@link #bounds}, which is enough to drive both the default broad-phase {@link #overlaps} check
 * and grid/tile-map queries such as {@link GridCollisions#isBlocked}.
 *
 * <p>Extends {@link EntityComponent} so any concrete collider attached to a {@link
 * com.cryptroot.core.world.WorldEntity} via {@code .with(SomeCollider.class, instance)}
 * auto-registers under {@code Collider.class} too — a future system that reacts to collisions can
 * iterate {@code world.entities()} filtering on this interface without knowing about concrete shape
 * types.
 */
public interface Collider extends EntityComponent {

  /**
   * Writes this collider's current axis-aligned bounding box into {@code out} and returns it.
   *
   * <p>Implementations that track a moving entity (see {@link BoxCollider}) must compute this live
   * from the entity's current position — never cache a stale box.
   */
  Rectangle bounds(Rectangle out);

  /**
   * Returns {@code true} if this collider's bounds overlap {@code other}'s bounds.
   *
   * <p>This is a broad-phase (AABB-only) test. Shapes that need exact narrow-phase overlap (e.g. a
   * circle-vs-circle distance check) should override this method; the default is still correct as a
   * conservative first pass for any shape, since {@link #bounds} is always an AABB.
   */
  default boolean overlaps(Collider other) {
    Objects.requireNonNull(other, "other must not be null");
    Rectangle a = bounds(new Rectangle());
    Rectangle b = other.bounds(new Rectangle());
    return a.overlaps(b);
  }
}
