package com.cryptroot.core.physics;

import com.cryptroot.core.event.Signal;
import com.cryptroot.core.world.EntityComponent;
import com.cryptroot.core.world.WorldEntity;

/**
 * Opt-in enter/exit overlap notifications for a {@link Collider}-carrying entity, fired by {@link
 * CollisionSystem} — the framework form of Unity's {@code OnTriggerEnter}/{@code OnTriggerExit}.
 *
 * <p>Attach alongside a {@link Collider} component (a single class may implement both {@link
 * Collider} and this interface, or a listener may be registered separately) to be notified when
 * this entity's collider starts or stops overlapping another entity's collider. An entity with a
 * {@link Collider} but no {@code CollisionListener} is still checked for overlaps — so the
 * <em>other</em> side of a pair can still be notified — it simply receives no callbacks of its own.
 */
public interface CollisionListener extends EntityComponent {

  /** Fires with the other entity when an overlap with it begins. */
  Signal<WorldEntity> onCollisionEnter();

  /** Fires with the other entity when a previously-overlapping pair stops overlapping. */
  Signal<WorldEntity> onCollisionExit();
}
