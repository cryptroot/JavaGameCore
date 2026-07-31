package com.cryptroot.core.ui.layout;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cryptroot.core.ui.BoundedWidget;
import com.cryptroot.core.ui.ScissorRegion;
import com.cryptroot.core.ui.UiWidget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base class for widgets whose only job is to size and position other {@link LayoutElement}s.
 *
 * <p>A container has no appearance of its own — for a visible box, put a container inside a {@link
 * com.cryptroot.core.ui.Panel Panel} via {@code Panel.setContent}. Keeping the two separate is why
 * panel chrome (title bars, tab strips) composes with layout instead of fighting it.
 *
 * <h3>What subclasses implement</h3>
 *
 * Exactly one method, {@link #arrange(Rectangle)}, which receives the content rectangle (this
 * container's frame minus its {@linkplain #padding(Insets) padding}) and calls {@link
 * LayoutElement#setBounds} on each {@linkplain #managed() managed child}. Everything else — child
 * registration, the lifecycle cascade, clipping, hit-testing — is inherited.
 *
 * <h3>Configuration</h3>
 *
 * The setters are fluent and return {@code this}, so a whole tree can be written as one expression:
 *
 * <pre>{@code
 * uiLayer.setRoot(new VStack().padding(Insets.all(20f)).spacing(16f)
 *         .add(topBar())
 *         .add(new HStack().spacing(16f).stretchCross(true)
 *                 .add(leftPanel, 1f)
 *                 .add(rightPanel, 1f), 1f)
 *         .add(bottomPanel, 1f));
 * }</pre>
 *
 * <h3>Rebuilding</h3>
 *
 * {@link #removeAll()} then re-adding is the intended way to refresh a data-driven list. That is
 * cheaper and safer than clearing the whole {@link com.cryptroot.core.ui.UiLayer UiLayer} and
 * rebuilding every widget, which also discards z-order and any focus state.
 *
 * @param <SELF> the concrete container type, so the fluent setters return it rather than this base
 *     class and a chain like {@code new VStack().spacing(6f)} still has type {@code VStack}
 */
public abstract class LayoutContainer<SELF extends LayoutContainer<SELF>> extends BoundedWidget
    implements Clippable {

  private final List<LayoutElement> managed = new ArrayList<>();

  private Insets padding = Insets.NONE;
  private float spacing = 0f;
  private int align = Align.topLeft;
  private boolean stretchCross;
  private boolean clipChildren;

  private final ScissorRegion scissor = new ScissorRegion();
  private final Rectangle contentScratch = new Rectangle();
  private final Vector2 measureScratch = new Vector2();

  /** {@code true} while a scissor pushed by {@link #doDraw} is open. */
  private boolean clipOpen;

  /**
   * This container as its concrete type. Safe because {@code SELF} is bound to the implementing
   * class by every subclass in this package.
   */
  @SuppressWarnings("unchecked")
  private SELF self() {
    return (SELF) this;
  }

  // -------------------------------------------------------------------------
  // Child management
  // -------------------------------------------------------------------------

  /**
   * Adds {@code child} as a layout-managed child. It is also registered for the standard {@link
   * com.cryptroot.core.ui.CompositeWidget CompositeWidget} lifecycle (layout, draw, hover, reset).
   *
   * @return this container, for chaining
   */
  public SELF add(LayoutElement child) {
    Objects.requireNonNull(child, "child must not be null");
    managed.add(child);
    addChild(child);
    return self();
  }

  /**
   * Adds {@code child} and sets its {@linkplain LayoutElement#growWeight() grow weight}, so it
   * takes a share of this container's leftover main-axis space.
   *
   * @param growWeight relative share of leftover space; {@code 0} means natural size only
   * @return this container, for chaining
   * @throws IllegalArgumentException if {@code child} does not support a settable grow weight
   */
  public SELF add(LayoutElement child, float growWeight) {
    Objects.requireNonNull(child, "child must not be null");
    if (!(child instanceof BoundedWidget bounded)) {
      throw new IllegalArgumentException(
          "grow weight can only be set on a BoundedWidget; "
              + child.getClass().getSimpleName()
              + " must override growWeight() itself");
    }
    bounded.setGrowWeight(growWeight);
    return add(child);
  }

  /** Removes every layout-managed child. Other registered children (if any) are left alone. */
  public void removeAll() {
    for (LayoutElement child : managed) {
      removeChild(child);
    }
    managed.clear();
  }

  /** Unmodifiable view of the layout-managed children, in insertion order. */
  protected final List<LayoutElement> managed() {
    return Collections.unmodifiableList(managed);
  }

  /** Number of layout-managed children. */
  public final int childCount() {
    return managed.size();
  }

  // -------------------------------------------------------------------------
  // Fluent configuration
  // -------------------------------------------------------------------------

  /** Sets the inset between this container's frame and its content area. */
  public SELF padding(Insets insets) {
    this.padding = Objects.requireNonNull(insets, "insets must not be null");
    return self();
  }

  /** Sets the gap inserted between adjacent children along the main axis. */
  public SELF spacing(float gap) {
    this.spacing = Math.max(0f, gap);
    return self();
  }

  /**
   * Sets how children are aligned, as a libGDX {@link Align} bitmask. The horizontal and vertical
   * halves are interpreted as main- and cross-axis alignment depending on the container's
   * orientation. Defaults to {@link Align#topLeft}.
   */
  public SELF align(int gdxAlign) {
    this.align = gdxAlign;
    return self();
  }

  /**
   * When {@code true}, children are stretched to fill the container's cross axis rather than
   * keeping their natural cross size. This is what makes a row of panels share a common height.
   */
  public SELF stretchCross(boolean stretch) {
    this.stretchCross = stretch;
    return self();
  }

  /** Fluent alias for {@link BoundedWidget#setGrowWeight(float)}. */
  public SELF grow(float weight) {
    setGrowWeight(weight);
    return self();
  }

  /**
   * When {@code true}, children are clipped to this container's content area, so content larger
   * than the container is cut off at its edge instead of overflowing across the screen.
   *
   * <p>Requires a clip context, which {@link com.cryptroot.core.ui.UiLayer UiLayer} supplies
   * automatically. Clipping costs two batch flushes per drawn container.
   */
  public SELF clipChildren(boolean clip) {
    this.clipChildren = clip;
    return self();
  }

  // -------------------------------------------------------------------------
  // Accessors for subclasses
  // -------------------------------------------------------------------------

  protected final Insets padding() {
    return padding;
  }

  protected final float spacing() {
    return spacing;
  }

  protected final int align() {
    return align;
  }

  protected final boolean stretchCross() {
    return stretchCross;
  }

  /**
   * Writes this container's content rectangle — {@link #frame} minus {@link #padding()} — into
   * {@code out}. Width and height are clamped at zero so an over-padded container degrades to an
   * empty content area instead of an inverted rectangle.
   */
  protected final Rectangle contentRect(Rectangle out) {
    return out.set(
        frame.x + padding.left(),
        frame.y + padding.bottom(),
        Math.max(0f, frame.width - padding.horizontal()),
        Math.max(0f, frame.height - padding.vertical()));
  }

  /** Shared scratch vector for measuring children; contents are not preserved across calls. */
  protected final Vector2 measureScratch() {
    return measureScratch;
  }

  // -------------------------------------------------------------------------
  // LayoutElement / BoundedWidget
  // -------------------------------------------------------------------------

  /**
   * Sealed: sets {@link #bounds} to the whole frame, then hands the padded content rectangle to
   * {@link #arrange(Rectangle)}. Children's own {@code layout()} calls follow automatically.
   */
  @Override
  protected final void doBoundedLayout() {
    if (frame.width <= 0f || frame.height <= 0f) {
      preferredSize(measureScratch);
      if (frame.width <= 0f) frame.width = measureScratch.x;
      if (frame.height <= 0f) frame.height = measureScratch.y;
    }
    bounds.set(frame);
    contentRect(contentScratch);
    scissor.setBounds(
        contentScratch.x, contentScratch.y, contentScratch.width, contentScratch.height);
    arrange(contentScratch);
  }

  /**
   * Subclass positions and sizes every {@linkplain #managed() managed child} inside {@code content}
   * by calling {@link LayoutElement#setBounds} on each.
   *
   * @param content this container's frame minus its padding; do not retain, it is reused scratch
   */
  protected abstract void arrange(Rectangle content);

  // -------------------------------------------------------------------------
  // Clipping
  // -------------------------------------------------------------------------

  /**
   * Forwards the clip context to this container's scissor and on to any {@link Clippable} child.
   */
  @Override
  public void setClipContext(Viewport viewport, Camera camera) {
    scissor.setClipContext(viewport, camera);
    for (UiWidget child : children()) {
      if (child instanceof Clippable clippable) {
        clippable.setClipContext(viewport, camera);
      }
    }
  }

  /** Opens the clip region, if enabled, before children are drawn. */
  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    clipOpen = clipChildren && scissor.begin(batch);
  }

  /** Closes the clip region opened by {@link #doDraw}. */
  @Override
  protected void doAfterDraw(PolygonSpriteBatch batch) {
    if (clipOpen) {
      scissor.end(batch);
      clipOpen = false;
    }
  }

  // -------------------------------------------------------------------------
  // Input — a container is transparent; only its children respond
  // -------------------------------------------------------------------------

  /**
   * Suppresses hover and hit-testing outside this container's bounds when clipping is on, so a
   * child scrolled out of view cannot be hovered or clicked through the clip edge.
   */
  @Override
  public void updateHover(float worldX, float worldY) {
    hovered = bounds.contains(worldX, worldY);
    if (clipChildren && !hovered) {
      for (UiWidget child : children()) child.clearHover();
      return;
    }
    for (UiWidget child : children()) child.updateHover(worldX, worldY);
  }

  @Override
  public boolean hit(float worldX, float worldY) {
    if (clipChildren && !bounds.contains(worldX, worldY)) return false;
    // Reverse order (last added draws on top, so it is tested first) via CompositeWidget's helper,
    // which also records the consumer so hitFocusable() can reach a nested Focusable child.
    return hitChildren(worldX, worldY);
  }

  @Override
  public boolean scrolled(float worldX, float worldY, float amountX, float amountY) {
    if (clipChildren && !bounds.contains(worldX, worldY)) return false;
    return super.scrolled(worldX, worldY, amountX, amountY);
  }
}
