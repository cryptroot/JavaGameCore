package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * A floating pop-up menu that auto-sizes to fit its entries.
 *
 * <p>Entries are added via {@link #addEntry(String, Runnable)}. The widget automatically grows
 * wider and taller with each additional entry — width matches the widest label (plus padding) and
 * height equals {@code entries × ROW_HEIGHT + top/bottom padding}.
 *
 * <p>The menu is hidden by default. Call {@link #show(float, float)} to anchor its top-left corner
 * at a world / UI coordinate and reveal it; call {@link #hide()} to dismiss it. Clicking an entry
 * fires its action and then hides the menu automatically. Clicking outside the menu's bounds while
 * it is open dismisses it without consuming the event, so the underlying entity or widget can still
 * receive the click.
 *
 * <p>While visible the menu renders on top of whatever z-order it was added at and is opaque
 * (absorbs pointer events inside its bounds).
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * PopupMenu menu = new PopupMenu(game.getPixel(), game.getFontBody());
 * menu.addEntry("Inspect", () -> Gdx.app.log("Menu", "Inspect clicked"));
 * menu.addEntry("Battle",  () -> Gdx.app.log("Menu", "Battle clicked"));
 * menu.addEntry("Dismiss", menu::hide);
 * uiLayer.add(menu, 3);          // high z-order so it renders on top
 *
 * // later, on click:
 * menu.show(clickUiX, clickUiY); // anchor top-left at cursor
 * }</pre>
 */
public final class PopupMenu extends BoundedWidget {

  // -------------------------------------------------------------------------
  // Visual constants
  // -------------------------------------------------------------------------

  private static final Color COLOR_BG = new Color(0.08f, 0.08f, 0.14f, 0.96f);
  private static final Color COLOR_BORDER = new Color(0.55f, 0.55f, 0.75f, 1f);
  private static final Color COLOR_ROW_HOVER = new Color(0.25f, 0.30f, 0.50f, 0.80f);
  private static final Color COLOR_LABEL = new Color(0.90f, 0.90f, 0.95f, 1f);

  private static final float BORDER_THICKNESS = 1f;
  private static final float PAD_H = 20f; // left/right padding inside each row
  private static final float PAD_V = 10f; // top/bottom menu padding
  private static final float ROW_H = 44f; // height of each row
  private static final float MIN_W = 180f; // minimum menu width

  // -------------------------------------------------------------------------
  // Per-entry data
  // -------------------------------------------------------------------------

  private static final class Entry {
    final String label;
    final Runnable action;
    final Rectangle rowBounds = new Rectangle();

    Entry(String label, Runnable action) {
      this.label = label;
      this.action = action;
    }
  }

  // -------------------------------------------------------------------------
  // Fields
  // -------------------------------------------------------------------------

  private final List<Entry> entries = new ArrayList<>();
  private final List<TextLabel> rowLabels = new ArrayList<>();

  private final BitmapFont font;
  private final GlyphLayout measure = new GlyphLayout();

  // bg and rowHighlight are drawn manually in doDraw() so we can control
  // layering: bg first, then highlight, then border + labels (children).
  private final PixelRect bg;
  private final PixelRect rowHighlight;
  private final PixelBorder border;

  private float anchorX;
  private float anchorY;
  private boolean visible = false;
  private int hoveredRow = -1;

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  /**
   * @param pixel 1×1 white texture used for solid-rect drawing
   * @param font font used for entry labels
   */
  public PopupMenu(Texture pixel, BitmapFont font) {
    this.font = font;
    bg = new PixelRect(pixel, COLOR_BG);
    rowHighlight = new PixelRect(pixel, COLOR_ROW_HOVER);
    border = new PixelBorder(pixel, BORDER_THICKNESS, COLOR_BORDER);

    // border is the only child registered at construction time.
    // Row TextLabels are appended as children in addEntry().
    // Draw order: doDraw (bg → highlight) → children (border → labels).
    addChild(border);
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Appends a new row to the menu. May be called at any time; call {@link #layout()} afterwards if
   * entries are added while the menu is already visible.
   *
   * @param label text displayed in the row
   * @param action callback fired when the row is clicked
   */
  public void addEntry(String label, Runnable action) {
    entries.add(new Entry(label, action));
    TextLabel lbl = new TextLabel(font, label, 0f, 0f, COLOR_LABEL);
    rowLabels.add(lbl);
    addChild(lbl);
  }

  /**
   * Shows the menu with its top-left corner at {@code (anchorX, anchorY)} in UI / world-coordinate
   * space.
   */
  public void show(float anchorX, float anchorY) {
    this.anchorX = anchorX;
    this.anchorY = anchorY;
    visible = true;
    layout();
  }

  /** Hides the menu and clears hover state. */
  public void hide() {
    visible = false;
    hoveredRow = -1;
  }

  // -------------------------------------------------------------------------
  // UiWidget overrides
  // -------------------------------------------------------------------------

  @Override
  public boolean isVisible() {
    return visible;
  }

  @Override
  public void updateHover(float worldX, float worldY) {
    if (!visible) {
      hoveredRow = -1;
      return;
    }
    hovered = bounds.contains(worldX, worldY);
    hoveredRow = -1;
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).rowBounds.contains(worldX, worldY)) {
        hoveredRow = i;
        break;
      }
    }
  }

  /**
   * Consumes clicks inside the menu and fires the matching row's action. Clicks outside the bounds
   * dismiss the menu without consuming the event, so the underlying widget or entity can still
   * receive it.
   */
  @Override
  public boolean hit(float worldX, float worldY) {
    if (!visible) return false;
    if (!bounds.contains(worldX, worldY)) {
      // Click outside — dismiss and let the event fall through.
      hide();
      return false;
    }
    // Click inside — fire the row action (if any) and dismiss.
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).rowBounds.contains(worldX, worldY)) {
        entries.get(i).action.run();
        hide();
        return true;
      }
    }
    return true; // Consumed: click was on padding / border area
  }

  // -------------------------------------------------------------------------
  // BoundedWidget hooks
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    if (entries.isEmpty()) {
      bounds.set(anchorX, anchorY, 0f, 0f);
      return;
    }

    // Compute the required width from the widest label.
    float maxLabelW = MIN_W - 2f * PAD_H;
    for (Entry e : entries) {
      measure.setText(font, e.label);
      if (measure.width > maxLabelW) maxLabelW = measure.width;
    }

    float menuW = maxLabelW + 2f * PAD_H;
    float menuH = entries.size() * ROW_H + 2f * PAD_V;

    // Anchor is the top-left corner; GDX Y is bottom-up, so bottom = top - height.
    float left = anchorX;
    float bottom = anchorY - menuH;

    bounds.set(left, bottom, menuW, menuH);
    bg.setBounds(left, bottom, menuW, menuH);
    border.setBounds(left, bottom, menuW, menuH);

    // Lay out rows from top to bottom.
    for (int i = 0; i < entries.size(); i++) {
      float rowBottom = bottom + menuH - PAD_V - (i + 1) * ROW_H;
      entries.get(i).rowBounds.set(left, rowBottom, menuW, ROW_H);

      // Vertically centre the baseline within the row.
      float baselineY = rowBottom + ROW_H * 0.5f + font.getCapHeight() * 0.5f;
      rowLabels.get(i).setPosition(left + PAD_H, baselineY);
    }
  }

  @Override
  protected void doBoundedReset() {
    hoveredRow = -1;
  }

  // -------------------------------------------------------------------------
  // CompositeWidget draw hook
  // -------------------------------------------------------------------------

  /**
   * Draws the background and (when a row is hovered) the highlight rectangle before the children
   * (border, labels) are drawn by the superclass.
   */
  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    bg.draw(batch);
    if (hoveredRow >= 0) {
      Rectangle r = entries.get(hoveredRow).rowBounds;
      rowHighlight.setBounds(r.x, r.y, r.width, r.height);
      rowHighlight.draw(batch);
    }
    // border and row TextLabels are children and will be drawn by
    // CompositeWidget.draw() immediately after this method returns.
  }
}
