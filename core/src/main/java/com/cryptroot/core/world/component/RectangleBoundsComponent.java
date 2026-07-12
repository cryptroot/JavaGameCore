package com.cryptroot.core.world.component;

import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.world.BoundsComponent;
import com.cryptroot.core.world.PositionComponent;
import java.util.Objects;

/**
 * Hit-tests an entity using a fixed axis-aligned {@link Rectangle}.
 *
 * <p>Implements both {@link BoundsComponent} and {@link PositionComponent}: {@link #moveTo}
 * repositions the rectangle's bottom-left corner, keeping all spatial state consistent.
 *
 * <p>Suitable for non-Spine entities such as static props, invisible trigger zones, and world-space
 * UI hotspots.
 */
public final class RectangleBoundsComponent implements BoundsComponent, PositionComponent {

  private final Rectangle bounds;

  /**
   * @param x bottom-left world X
   * @param y bottom-left world Y
   * @param width rectangle width in world units
   * @param height rectangle height in world units
   */
  public RectangleBoundsComponent(float x, float y, float width, float height) {
    if (width <= 0f) {
      throw new IllegalArgumentException("width must be positive: " + width);
    }
    if (height <= 0f) {
      throw new IllegalArgumentException("height must be positive: " + height);
    }
    this.bounds = new Rectangle(x, y, width, height);
  }

  /** Constructs from an existing rectangle (copied — the original is not modified). */
  public RectangleBoundsComponent(Rectangle rect) {
    Objects.requireNonNull(rect, "rect must not be null");
    this.bounds = new Rectangle(rect);
  }

  // -------------------------------------------------------------------------
  // BoundsComponent
  // -------------------------------------------------------------------------

  @Override
  public boolean containsPoint(float wx, float wy) {
    return bounds.contains(wx, wy);
  }

  @Override
  public Rectangle bounds(Rectangle out) {
    out.set(bounds);
    return out;
  }

  // -------------------------------------------------------------------------
  // PositionComponent
  // -------------------------------------------------------------------------

  @Override
  public float x() {
    return bounds.x;
  }

  @Override
  public float y() {
    return bounds.y;
  }

  @Override
  public void moveTo(float x, float y) {
    bounds.setPosition(x, y);
  }
}
