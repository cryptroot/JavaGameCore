package com.cryptroot.core.world;

import com.badlogic.gdx.math.Rectangle;

/**
 * Component that occupies a region in world space and can be hit-tested.
 *
 * <p>Only entities with this component participate in pointer hover detection and click dispatch
 * inside {@link WorldEntityLayer}.
 */
public interface BoundsComponent extends EntityComponent {
  /** Returns {@code true} if the world-space point {@code (wx, wy)} is inside this entity. */
  boolean containsPoint(float wx, float wy);

  /**
   * Writes the entity's axis-aligned bounding box into {@code out} and returns it. Used by {@link
   * WorldEntityLayer} to frame the FBO capture for {@link
   * com.cryptroot.core.render.SelectionOutlineRenderer}.
   */
  Rectangle bounds(Rectangle out);
}
