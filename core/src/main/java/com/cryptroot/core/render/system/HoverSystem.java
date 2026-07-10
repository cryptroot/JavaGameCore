package com.cryptroot.core.render.system;

import com.cryptroot.core.render.SelectionOutlineRenderer;
import com.cryptroot.core.world.BoundsComponent;
import com.cryptroot.core.world.ClickableComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;

/**
 * Stateful system that tracks which entity is currently hovered, fires {@link
 * ClickableComponent#onHoverEnter()} / {@link ClickableComponent#onHoverExit()} on transitions, and
 * maintains the outline fade alpha.
 *
 * <p>One instance per scene. Call {@link #reset()} when the scene is torn down (screen hide) to
 * prevent stale references.
 */
public final class HoverSystem {

  private WorldEntity hoveredEntity;
  private WorldEntity outlineTarget;
  private float hoverAlpha;

  /**
   * Detects which entity (if any) is under the world-space cursor and advances the outline fade
   * alpha.
   *
   * @param worldX world-space cursor X
   * @param worldY world-space cursor Y
   * @param delta frame delta used to advance the fade alpha
   */
  public void process(World world, float worldX, float worldY, float delta) {
    WorldEntity found = hitTest(world, worldX, worldY);

    if (found != hoveredEntity) {
      if (hoveredEntity != null) {
        hoveredEntity.get(ClickableComponent.class).ifPresent(c -> c.onHoverExit().emit());
      }
      if (found != null) {
        found.get(ClickableComponent.class).ifPresent(c -> c.onHoverEnter().emit());
      }
      hoveredEntity = found;
    }

    if (hoveredEntity != null && outlineTarget != hoveredEntity) {
      outlineTarget = hoveredEntity;
      hoverAlpha = 0f;
    }
    if (hoveredEntity != null) {
      hoverAlpha = Math.min(1f, hoverAlpha + SelectionOutlineRenderer.FADE_SPEED * delta);
    } else {
      hoverAlpha = Math.max(0f, hoverAlpha - SelectionOutlineRenderer.FADE_SPEED * delta);
      if (hoverAlpha <= 0f) outlineTarget = null;
    }
  }

  public float hoverAlpha() {
    return hoverAlpha;
  }

  public WorldEntity outlineTarget() {
    return outlineTarget;
  }

  /**
   * The entity currently under the cursor, or {@code null} if none. Unlike {@link
   * #outlineTarget()}, this clears immediately on hover-exit (it does not linger through the
   * fade-out), so it is the right source for instant, non-faded outline selection.
   */
  public WorldEntity hoveredEntity() {
    return hoveredEntity;
  }

  /** Clears hover and outline state. Call when the scene is hidden or torn down. */
  public void reset() {
    hoveredEntity = null;
    outlineTarget = null;
    hoverAlpha = 0f;
  }

  private static WorldEntity hitTest(World world, float wx, float wy) {
    var entities = world.entities();
    for (int i = entities.size() - 1; i >= 0; i--) {
      WorldEntity e = entities.get(i);
      if (e.has(BoundsComponent.class)
          && e.get(BoundsComponent.class).get().containsPoint(wx, wy)) {
        return e;
      }
    }
    return null;
  }
}
