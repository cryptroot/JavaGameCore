package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.ui.layout.Insets;
import com.cryptroot.core.ui.layout.LayoutElement;
import java.util.Objects;

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
 * // Layout-managed interior — the panel sizes and positions its content:
 * Panel panel = new Panel(pixel);
 * panel.setContent(new VStack().padding(Insets.all(12f)).spacing(6f)
 *         .add(myButton)
 *         .add(mySlider));
 * parentStack.add(panel);
 *
 * // Or absolute coordinates, for a hand-positioned HUD:
 * Panel hud = new Panel(pixel, 80f, 120f, 900f, 650f);
 * hud.addWidget(myLabel);
 * uiLayer.add(hud, 0);
 * }</pre>
 *
 * <h3>Layout</h3>
 *
 * Geometry is the inherited {@link BoundedWidget#setBounds} contract: an outer rectangle with a
 * bottom-left origin, which a layout container may assign. {@link #setContent(LayoutElement)}
 * content is given exactly {@link #getContentBounds()} each pass, so it respects subclass chrome
 * insets automatically. Children added via {@link #addWidget} keep their own coordinates and are
 * laid out in the standard {@link CompositeWidget} cascade.
 *
 * <p>Note a plain {@code Panel} does not clip its children — content larger than the panel
 * overflows it. Wrap the content in a container with {@code clipChildren(true)} when that matters.
 */
public class Panel extends BoundedWidget {

  // Default visual style
  private static final Color COLOR_BG_DEFAULT = new Color(0.08f, 0.08f, 0.14f, 0.95f);
  private static final Color COLOR_BORDER_DEFAULT = new Color(0.45f, 0.45f, 0.60f, 1f);
  private static final float BORDER_THICKNESS_DEFAULT = 1f;

  private final PixelRect bg;
  private final PixelBorder border;

  /** Layout-managed content, assigned {@link #getContentBounds()} on every pass. */
  private LayoutElement content;

  /** Scratch rectangle so {@link #doBoundedLayout()} allocates nothing. */
  private final Rectangle contentScratch = new Rectangle();

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
   * Creates a panel with default colours and no geometry, for use inside a layout container that
   * will assign its bounds.
   */
  public Panel(Texture pixel) {
    this(pixel, 0f, 0f, 0f, 0f);
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
    Objects.requireNonNull(pixel, "pixel must not be null");
    Objects.requireNonNull(bgColor, "bgColor must not be null");
    Objects.requireNonNull(borderColor, "borderColor must not be null");
    setBounds(x, y, w, h);
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
   * Adds a widget as a child of this panel at whatever coordinates the widget itself carries. It
   * will participate in the standard {@link CompositeWidget} lifecycle (layout, draw, hover,
   * reset).
   *
   * <p>This is a convenience alias for the protected {@link #addChild} method. For content that
   * should fill and be positioned by the panel, prefer {@link #setContent(LayoutElement)}.
   */
  public void addWidget(UiWidget widget) {
    Objects.requireNonNull(widget, "widget must not be null");
    addChild(widget);
  }

  /**
   * Sets the panel's layout-managed content. On every layout pass {@code content} is assigned
   * exactly {@link #getContentBounds()}, so it automatically respects the chrome insets of
   * subclasses — a {@link CloseablePanel}'s title bar or a {@link TabbedPanel}'s tab strip — with
   * no knowledge of them.
   *
   * <p>This is the hinge between panel chrome and the layout containers: pass a {@link
   * com.cryptroot.core.ui.layout.VStack VStack} and the panel's interior lays itself out.
   *
   * <pre>{@code
   * Panel rooms = new Panel(pixel);
   * rooms.setContent(new VStack().padding(Insets.all(12f)).spacing(6f).add(roomRows()));
   * }</pre>
   *
   * <p>Replaces any previously set content. Pass {@code null} to clear it.
   */
  public void setContent(LayoutElement newContent) {
    if (content != null) {
      removeChild(content);
    }
    content = newContent;
    if (newContent != null) {
      addChild(newContent);
    }
  }

  /** Returns the layout-managed content set by {@link #setContent}, or {@code null}. */
  public LayoutElement getContent() {
    return content;
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
    return frame.x;
  }

  /** Returns the bottom edge of the panel in world coordinates. */
  public float getPanelY() {
    return frame.y;
  }

  /** Returns the width of the panel. */
  public float getPanelW() {
    return frame.width;
  }

  /** Returns the height of the panel. */
  public float getPanelH() {
    return frame.height;
  }

  /**
   * The space this panel's chrome consumes on each edge. Zero for a plain {@code Panel}; subclasses
   * with a title bar ({@link CloseablePanel}) or a tab strip ({@link TabbedPanel}) report it here.
   *
   * <p>Overriding this one method is all a subclass needs to do for both {@link
   * #getContentBounds()} and {@link #preferredSize(Vector2)} to account for its chrome — the two
   * cannot drift apart.
   */
  protected Insets chromeInsets() {
    return Insets.NONE;
  }

  /**
   * Returns the inset content area of this panel as a {@link Rectangle}. For a plain {@code Panel}
   * this is identical to the full bounds; subclasses shrink it to exclude chrome like tab strips.
   *
   * <p>The returned rectangle is a fresh instance on every call — it is not cached, so callers can
   * store it without aliasing concerns. Use {@link #getContentBounds(Rectangle)} on layout paths.
   */
  public Rectangle getContentBounds() {
    return getContentBounds(new Rectangle());
  }

  /**
   * Allocation-free {@link #getContentBounds()}: writes the content area into {@code out} and
   * returns it.
   */
  public Rectangle getContentBounds(Rectangle out) {
    return contentBoundsFor(frame, chromeInsets(), out);
  }

  /**
   * The content rectangle for a panel of the given outer rectangle and chrome insets, with width
   * and height clamped at zero.
   *
   * <p>Extracted as a pure static so panel chrome arithmetic is unit-testable without constructing
   * a {@link Texture}.
   *
   * @return {@code out}, for chaining
   */
  public static Rectangle contentBoundsFor(Rectangle frame, Insets chrome, Rectangle out) {
    Objects.requireNonNull(frame, "frame must not be null");
    Objects.requireNonNull(chrome, "chrome must not be null");
    Objects.requireNonNull(out, "out must not be null");
    return out.set(
        frame.x + chrome.left(),
        frame.y + chrome.bottom(),
        Math.max(0f, frame.width - chrome.horizontal()),
        Math.max(0f, frame.height - chrome.vertical()));
  }

  /**
   * Natural size: the {@linkplain #setContent content}'s natural size grown by {@link
   * #chromeInsets()}. Falls back to the assigned frame size when there is no layout-managed
   * content, since a decorative panel has no intrinsic size of its own.
   */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    if (content == null) {
      return super.preferredSize(out);
    }
    content.preferredSize(out);
    Insets chrome = chromeInsets();
    return out.set(out.x + chrome.horizontal(), out.y + chrome.vertical());
  }

  // -------------------------------------------------------------------------
  // BoundedWidget
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    bounds.set(frame);
    bg.setBounds(frame.x, frame.y, frame.width, frame.height);
    border.setBounds(frame.x, frame.y, frame.width, frame.height);
    if (content != null) {
      getContentBounds(contentScratch);
      content.setBounds(
          contentScratch.x, contentScratch.y, contentScratch.width, contentScratch.height);
    }
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
    // Offer the point to children (topmost first) via CompositeWidget's helper, which also records
    // the consumer so a nested Focusable can still be found by hitFocusable(). Must NOT call
    // super.hit() here: BoundedWidget.hit() is a raw bounds check that would short-circuit before
    // any child is tested.
    if (hitChildren(worldX, worldY)) return true;
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
    return visible && opaque && contains(worldX, worldY);
  }

  /**
   * An invisible panel contains nothing, so it never absorbs hover or occludes what is beneath it.
   */
  @Override
  public boolean contains(float worldX, float worldY) {
    return visible && bounds.contains(worldX, worldY);
  }

  @Override
  public void doDraw(PolygonSpriteBatch batch) {
    // bg and border are registered children and are drawn by the CompositeWidget
    // delegation in draw(); nothing extra needed here.
  }
}
