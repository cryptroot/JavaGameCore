package com.cryptroot.core.ui;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.ui.layout.LayoutElement;

/**
 * Abstract base for widgets that occupy a rectangle and track a hovered state, consolidating the
 * repeated {@link Rectangle} + {@code updateHover}/{@code hit}/{@code reset} boilerplate shared by
 * button-like interactive widgets.
 *
 * <h3>frame vs. bounds</h3>
 *
 * Two rectangles, with distinct roles — conflating them is the mistake this split exists to
 * prevent:
 *
 * <ul>
 *   <li>{@link #frame} is the layout <em>input</em>: the outer rectangle assigned from outside via
 *       {@link #setBounds}, either by a {@link com.cryptroot.core.ui.layout.LayoutContainer
 *       LayoutContainer} or directly by game code.
 *   <li>{@link #bounds} is the layout <em>output</em>: the hit-test area, computed in {@link
 *       #doBoundedLayout()}. Usually identical to {@code frame}; widgets that deliberately differ
 *       (a slider with grab slop, a button sized to its own text) set it to something else.
 * </ul>
 *
 * <p>Because {@code setBounds} and {@code setPosition} are {@code final} here, every subclass
 * shares one unambiguous geometry meaning — outer rectangle, bottom-left origin — which is what
 * makes generic layout containers possible. Subclasses read {@code frame} in {@link
 * #doBoundedLayout()} instead of keeping private coordinate fields.
 *
 * <p>Subclasses inherit:
 *
 * <ul>
 *   <li>{@link #hovered} — {@code true} when the cursor is inside {@link #bounds}
 *   <li>Default {@link #updateHover} — {@code hovered = bounds.contains(…)}
 *   <li>Default {@link #hit} — {@code return bounds.contains(…)}
 *   <li>Default {@link #preferredSize} — the current {@code frame} size; override where the widget
 *       has a natural size of its own (measured text, a fixed row height, …)
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
public abstract class BoundedWidget extends CompositeWidget implements LayoutElement {

  /**
   * The outer rectangle assigned via {@link #setBounds} — the layout input. Read this in {@link
   * #doBoundedLayout()}.
   */
  protected final Rectangle frame = new Rectangle();

  /** The rectangular hit-test area — the layout output. Set in {@link #doBoundedLayout()}. */
  protected final Rectangle bounds = new Rectangle();

  /** {@code true} when the cursor is inside {@link #bounds}. */
  protected boolean hovered;

  /** Share of a container's leftover main-axis space; see {@link #setGrowWeight(float)}. */
  private float growWeight;

  // -------------------------------------------------------------------------
  // LayoutElement (final — one geometry meaning for every subclass)
  // -------------------------------------------------------------------------

  /**
   * Records the outer rectangle into {@link #frame}. Applied by the next {@link #layout()} call.
   *
   * <p>Sealed: a subclass that needs to react to being resized does so in {@link
   * #doBoundedLayout()}, which always runs afterwards.
   */
  @Override
  public final void setBounds(float x, float y, float width, float height) {
    frame.set(x, y, width, height);
  }

  /** Moves {@link #frame}'s bottom-left corner, preserving its size. */
  @Override
  public final void setPosition(float x, float y) {
    frame.setPosition(x, y);
  }

  /**
   * Default: the current {@link #frame} size, i.e. "whatever I was last given".
   *
   * <p>Override in widgets with a natural size — a {@link Button} measures its label, a row
   * measures its font — otherwise a container has nothing to size the widget from and will collapse
   * it to zero on the first pass.
   */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    return out.set(frame.width, frame.height);
  }

  /** Returns a copy of the outer rectangle last assigned via {@link #setBounds}. */
  public Rectangle getFrame() {
    return new Rectangle(frame);
  }

  /**
   * Sets this widget's share of its container's leftover main-axis space. Zero (the default) means
   * the widget takes only its natural size.
   *
   * <p>Lives here rather than only on containers so that any widget can be told to fill — a {@link
   * Panel} that should expand to fill a row needs this just as much as a nested stack does.
   *
   * @param weight relative weight; negative values are clamped to zero
   */
  public void setGrowWeight(float weight) {
    this.growWeight = Math.max(0f, weight);
  }

  @Override
  public float growWeight() {
    return growWeight;
  }

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
   * Subclass computes {@link #bounds} from {@link #frame} and positions any children. Called by the
   * sealed {@link #doLayout()} before children's {@code layout()} methods are invoked.
   *
   * <p>Default: {@code bounds.set(frame)} — correct for any widget whose hit area is its whole
   * frame.
   */
  protected void doBoundedLayout() {
    bounds.set(frame);
  }

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
   * Clears {@link #hovered} and recurses into children, without relying on sentinel coordinates.
   */
  @Override
  public void clearHover() {
    hovered = false;
    super.clearHover();
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

  /** Side-effect-free bounds query — safe to call regardless of what {@link #hit} does. */
  @Override
  public boolean contains(float worldX, float worldY) {
    return bounds.contains(worldX, worldY);
  }
}
