package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;

/**
 * A rectangular background panel that groups and visually contains child widgets.
 *
 * <p>{@code Panel} extends {@link BoundedWidget}, giving it a hit-test rectangle, hover state, and
 * the standard {@code setBounds}-driven layout contract shared with other positioned composites
 * (e.g., {@link ProgressBar}).
 *
 * <h3>Opaque vs. pass-through</h3>
 *
 * By default the panel is <em>pass-through</em>: {@link #hit} returns {@code false} so pointer
 * events sink through to lower-z widgets. Call {@link #setOpaque(boolean)} with {@code true} to
 * make the panel absorb all clicks inside its bounds (nothing below it fires).
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * Panel panel = new Panel(pixel, 80f, 120f, 900f, 650f);
 * // add widgets to the panel:
 * panel.addWidget(myButton);
 * panel.addWidget(mySlider);
 * uiLayer.add(panel, 0);
 * }</pre>
 *
 * <h3>Layout</h3>
 *
 * Call {@link #setBounds(float, float, float, float)} before adding to a {@link UiLayer} (or before
 * calling {@link #layout()} manually). Children added via {@link #addWidget} are laid out in the
 * standard {@link CompositeWidget} cascade.
 */
public class Panel extends BoundedWidget {

  // Default visual style
  private static final Color COLOR_BG_DEFAULT = new Color(0.08f, 0.08f, 0.14f, 0.95f);
  private static final Color COLOR_BORDER_DEFAULT = new Color(0.45f, 0.45f, 0.60f, 1f);
  private static final float BORDER_THICKNESS_DEFAULT = 1f;

  private final PixelRect bg;
  private final PixelBorder border;

  private float x;
  private float y;
  private float w;
  private float h;

  private boolean opaque = false;
  private boolean visible = true;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Creates a panel with default background / border colours at the given world-space position and
   * size.
   */
  public Panel(Texture pixel, float x, float y, float w, float h) {
    this(
        pixel,
        x,
        y,
        w,
        h,
        COLOR_BG_DEFAULT.cpy(),
        COLOR_BORDER_DEFAULT.cpy(),
        BORDER_THICKNESS_DEFAULT);
  }

  /**
   * Creates a panel with custom visual style.
   *
   * @param pixel 1×1 white texture for solid-rect drawing
   * @param x left edge in world coordinates
   * @param y bottom edge in world coordinates
   * @param w panel width
   * @param h panel height
   * @param bgColor background fill colour (copied)
   * @param borderColor border colour (copied)
   * @param borderThickness border stroke width in world units
   */
  public Panel(
      Texture pixel,
      float x,
      float y,
      float w,
      float h,
      Color bgColor,
      Color borderColor,
      float borderThickness) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    bg = new PixelRect(pixel, bgColor);
    border = new PixelBorder(pixel, borderThickness, borderColor);
    // bg first (drawn behind), border last (drawn in front).
    addChild(bg);
    addChild(border);
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Repositions and resizes the panel. Call before {@link #layout()} or before being added to a
   * {@link UiLayer} (which calls {@code layout()} automatically).
   */
  public void setBounds(float x, float y, float w, float h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  /**
   * Adds a widget as a child of this panel. It will participate in the standard {@link
   * CompositeWidget} lifecycle (layout, draw, hover, reset).
   *
   * <p>This is a convenience alias for the protected {@link #addChild} method, making the Panel's
   * public API explicit.
   */
  public void addWidget(UiWidget widget) {
    addChild(widget);
  }

  /**
   * When {@code true}, the panel absorbs pointer events inside its bounds so that lower-z widgets
   * never receive them. When {@code false} (the default), hits pass through to widgets underneath.
   */
  public void setOpaque(boolean opaque) {
    this.opaque = opaque;
  }

  /** Returns whether this panel absorbs pointer events inside its bounds. */
  public boolean isOpaque() {
    return opaque;
  }

  /**
   * Shows or hides this panel. When hidden, {@link #draw} is skipped (via the {@link
   * CompositeWidget} visibility guard), and all pointer events ({@link #hit}, {@link #updateHover},
   * {@link #scrolled}) are suppressed so invisible panels never consume input.
   */
  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  /** Returns the left edge of the panel in world coordinates. */
  public float getPanelX() {
    return x;
  }

  /** Returns the bottom edge of the panel in world coordinates. */
  public float getPanelY() {
    return y;
  }

  /** Returns the width of the panel. */
  public float getPanelW() {
    return w;
  }

  /** Returns the height of the panel. */
  public float getPanelH() {
    return h;
  }

  /**
   * Returns the inset content area of this panel as a {@link Rectangle}. For a plain {@code Panel}
   * this is identical to the full bounds; subclasses (e.g., {@link TabbedPanel}) shrink it to
   * exclude chrome like tab strips.
   *
   * <p>The returned rectangle is a fresh instance on every call — it is not cached, so callers can
   * store it without aliasing concerns.
   */
  public Rectangle getContentBounds() {
    return new Rectangle(x, y, w, h);
  }

  // -------------------------------------------------------------------------
  // BoundedWidget
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    bounds.set(x, y, w, h);
    bg.setBounds(x, y, w, h);
    border.setBounds(x, y, w, h);
  }

  /**
   * Returns {@code true} only when the panel is {@linkplain #setOpaque(boolean) opaque}; otherwise
   * the click passes through to lower-z widgets.
   *
   * <p>Registered children (e.g. tab buttons in {@link TabbedPanel}) are always tested first
   * regardless of the opaque flag.
   */
  @Override
  public boolean hit(float worldX, float worldY) {
    if (!visible) return false;
    // Iterate registered children in reverse-insertion order (topmost first).
    // Must NOT call super.hit() here because BoundedWidget.hit() is a raw
    // bounds check that would short-circuit before any child is tested.
    var kids = children();
    for (int i = kids.size() - 1; i >= 0; i--) {
      if (kids.get(i).hit(worldX, worldY)) return true;
    }
    return opaque && bounds.contains(worldX, worldY);
  }

  /**
   * Sets {@link #hovered} and forwards to all registered children so that children such as tab
   * buttons receive hover updates.
   *
   * <p>Must NOT rely on {@code super.updateHover()} because {@link BoundedWidget#updateHover} only
   * sets the {@code hovered} flag and does not forward to children.
   */
  @Override
  public void updateHover(float worldX, float worldY) {
    if (!visible) {
      hovered = false;
      return;
    }
    hovered = bounds.contains(worldX, worldY);
    for (UiWidget c : children()) c.updateHover(worldX, worldY);
  }

  @Override
  public boolean scrolled(float worldX, float worldY, float amountX, float amountY) {
    if (!visible) return false;
    return super.scrolled(worldX, worldY, amountX, amountY);
  }

  /**
   * An opaque, visible panel occludes the world point inside its bounds so that lower-z widgets do
   * not receive hover or outline treatment there.
   */
  @Override
  public boolean blocksPointer(float worldX, float worldY) {
    return visible && opaque && bounds.contains(worldX, worldY);
  }

  @Override
  public void doDraw(PolygonSpriteBatch batch) {
    // bg and border are registered children and are drawn by the CompositeWidget
    // delegation in draw(); nothing extra needed here.
  }
}
