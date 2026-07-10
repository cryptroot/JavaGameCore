package com.cryptroot.core.ui;

import com.badlogic.gdx.math.Rectangle;

/**
 * Abstract base for widgets that have a rectangular hit bounds and a hovered state, consolidating
 * the repeated {@link Rectangle bounds} + {@code updateHover}/{@code hit}/{@code reset} boilerplate
 * present in button-like interactive widgets.
 *
 * <p>Subclasses inherit:
 *
 * <ul>
 *   <li>{@link #bounds} — the rectangular hit area, set in {@link #doBoundedLayout()}
 *   <li>{@link #hovered} — {@code true} when the cursor is inside {@link #bounds}
 *   <li>Default {@link #updateHover} — {@code hovered = bounds.contains(…)}
 *   <li>Default {@link #hit} — {@code return bounds.contains(…)}
 * </ul>
 *
 * <p>Layout and reset use renamed template methods so subclasses cannot accidentally bypass the
 * shared invariants:
 *
 * <ul>
 *   <li>Override {@link #doBoundedLayout()} instead of {@link #doLayout()}
 *   <li>Override {@link #doBoundedReset()} instead of {@link #doReset()}
 * </ul>
 */
public abstract class BoundedWidget extends CompositeWidget {

  /** The rectangular hit-test area. Set in {@link #doBoundedLayout()}. */
  protected final Rectangle bounds = new Rectangle();

  /** {@code true} when the cursor is inside {@link #bounds}. */
  protected boolean hovered;

  // -------------------------------------------------------------------------
  // CompositeWidget template-method hooks (sealed to enforce invariant)
  // -------------------------------------------------------------------------

  /** Sealed: delegates entirely to {@link #doBoundedLayout()}. */
  @Override
  protected final void doLayout() {
    doBoundedLayout();
  }

  /** Sealed: clears {@link #hovered} then calls {@link #doBoundedReset()}. */
  @Override
  protected final void doReset() {
    hovered = false;
    doBoundedReset();
  }

  // -------------------------------------------------------------------------
  // New subclass override points
  // -------------------------------------------------------------------------

  /**
   * Subclass computes {@link #bounds} and positions any children. Called by the sealed {@link
   * #doLayout()} before children's {@code layout()} methods are invoked. Default: no-op.
   */
  protected void doBoundedLayout() {}

  /**
   * Subclass clears its own transient state beyond {@link #hovered}. Called by the sealed {@link
   * #doReset()} after {@code hovered} is cleared. Default: no-op.
   */
  protected void doBoundedReset() {}

  // -------------------------------------------------------------------------
  // UiWidget defaults (may be overridden by subclasses)
  // -------------------------------------------------------------------------

  /** Default: {@code hovered = bounds.contains(worldX, worldY)}. */
  @Override
  public void updateHover(float worldX, float worldY) {
    hovered = bounds.contains(worldX, worldY);
  }

  /**
   * Returns a copy of this widget's hit bounds after the last {@link #layout()} call. Safe to store
   * — the returned instance is not backed by the internal {@link #bounds} field.
   */
  public Rectangle getBounds() {
    return new Rectangle(bounds);
  }

  /** Default: {@code return bounds.contains(worldX, worldY)}. */
  @Override
  public boolean hit(float worldX, float worldY) {
    return bounds.contains(worldX, worldY);
  }
}
