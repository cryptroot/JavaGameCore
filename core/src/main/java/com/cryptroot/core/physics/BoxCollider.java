package com.cryptroot.core.physics;

import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.world.PositionComponent;
import java.util.Objects;

/**
 * An axis-aligned bounding box {@link Collider}, anchored to a live {@link PositionComponent}
 * rather than owning its own position.
 *
 * <p>The box tracks {@code anchor.x()/y()} plus a fixed offset every time {@link #bounds} is
 * called, so it can never drift out of sync with the entity it belongs to — there is exactly one
 * source of truth for the entity's position. This mirrors the anchoring pattern already used by
 * {@link com.cryptroot.core.world.component.WorldHealthBarComponent}.
 *
 * <p>Deliberately does <em>not</em> implement {@link PositionComponent} itself: a {@link
 * com.cryptroot.core.world.WorldEntity} keeps only one registration per interface, so if both the
 * render component and this collider implemented {@link PositionComponent}, whichever was
 * registered first would silently "win" and the other would go stale.
 */
public final class BoxCollider implements Collider {

  private final PositionComponent anchor;
  private final float offsetX;
  private final float offsetY;
  private final float width;
  private final float height;

  /**
   * @param anchor the live position this collider tracks (typically the same object as the entity's
   *     render component)
   * @param offsetX box's bottom-left X offset from the anchor's X, in world units
   * @param offsetY box's bottom-left Y offset from the anchor's Y, in world units
   * @param width box width in world units
   * @param height box height in world units
   */
  public BoxCollider(
      PositionComponent anchor, float offsetX, float offsetY, float width, float height) {
    this.anchor = Objects.requireNonNull(anchor, "anchor must not be null");
    if (width <= 0f || height <= 0f) {
      throw new IllegalArgumentException(
          "width and height must be positive, got " + width + "x" + height);
    }
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.width = width;
    this.height = height;
  }

  @Override
  public Rectangle bounds(Rectangle out) {
    out.set(anchor.x() + offsetX, anchor.y() + offsetY, width, height);
    return out;
  }
}
