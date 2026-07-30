package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cryptroot.core.event.Signal;
import com.cryptroot.core.ui.layout.Clippable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A vertically scrollable list of text items, clipped to its own bounds.
 *
 * <p>Each item string is backed by a {@link TextLabel} that caches its glyph layout. Label
 * positions are updated in {@link #doDraw(PolygonSpriteBatch)} to reflect the current scroll
 * offset, so a layout is only rebaked when an item's text, position or colour actually changes.
 *
 * <p>Items can be scrolled with the mouse wheel. Clicking an item selects it and emits {@link
 * #onItemSelected}:
 *
 * <pre>{@code
 * ScrollList log = new ScrollList(skin, pixel, lines);
 * log.onItemSelected.connect(idx -> status = "Selected: " + lines.get(idx));
 * logPanel.setContent(log);
 * }</pre>
 *
 * <p>Items can be replaced at any time via {@link #setItems(List)}, which preserves nothing but the
 * geometry — scroll position and selection reset.
 *
 * <p><b>Selection is optional.</b> A freshly populated list has no selection ({@code -1}); it is
 * not forced onto the first row. A list used purely as a log viewer therefore shows no highlight
 * unless the user clicks one.
 *
 * <p><b>Clipping</b> is handled through {@link Clippable}: {@link UiLayer} supplies the viewport
 * and camera, so the constructor does not need them.
 *
 * <p><b>Labels as helpers, not children:</b> the {@link TextLabel} objects are held in a private
 * typed list and drawn manually inside the clip region. They are <em>not</em> registered as {@link
 * CompositeWidget} children, because only the rows — not the background or border — belong inside
 * the scissor, and the before-children / after-children template split cannot express that.
 */
public final class ScrollList extends BoundedWidget implements Clippable {

  private static final float ITEM_PADDING_H = 10f;
  private static final float ITEM_PADDING_V = 6f;
  private static final float SCROLL_SPEED = 1f;

  /** Returned by {@link #getSelectedIndex()} when nothing is selected. */
  public static final int NO_SELECTION = -1;

  private static final Color COLOR_ITEM_NORMAL = Color.WHITE;
  private static final Color COLOR_ITEM_HOVER = new Color(0.85f, 0.85f, 1.0f, 1f);
  private static final Color COLOR_SELECTED_BG = new Color(0.25f, 0.25f, 0.45f, 1f);

  /** Fires with the index of the item that was just clicked. */
  public final Signal<Integer> onItemSelected = new Signal<>();

  private final UiSkin skin;
  private final Texture pixel;
  private final ScissorRegion scissor = new ScissorRegion();

  private final PixelBorder border;
  private final PixelRect bg;

  private List<String> items = new ArrayList<>();
  private List<TextLabel> itemLabels = new ArrayList<>();

  private int selectedIndex = NO_SELECTION;
  private float scrollOffsetY = 0f;
  private float itemHeight;
  private float maxScrollY;
  private int hoveredIndex = NO_SELECTION;

  /**
   * Creates a list sized by its enclosing layout container.
   *
   * @param skin skin providing the font for item labels
   * @param pixel 1×1 white pixel texture for backgrounds and borders
   * @param items initial item list (copied)
   */
  public ScrollList(UiSkin skin, Texture pixel, List<String> items) {
    Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(pixel, "pixel must not be null");
    this.skin = skin;
    this.pixel = pixel;
    border = new PixelBorder(pixel, 2f, new Color(0.5f, 0.5f, 0.5f, 1f));
    bg = new PixelRect(pixel, new Color(0.10f, 0.10f, 0.16f, 1f));
    setItems(items);
  }

  /**
   * Creates a list at explicit world coordinates, for hand-positioned screens.
   *
   * <p>The {@code viewport} parameter is no longer needed — clipping context arrives via {@link
   * Clippable} — but is accepted so existing call sites keep working.
   */
  public ScrollList(
      UiSkin skin,
      Texture pixel,
      Viewport viewport,
      float listX,
      float listY,
      float listW,
      float listH,
      List<String> items) {
    this(skin, pixel, items);
    setBounds(listX, listY, listW, listH);
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Replaces the item list, resetting scroll position and clearing the selection.
   *
   * <p>Does <em>not</em> trigger a layout pass: the enclosing {@link CompositeWidget} cascade or
   * {@link UiLayer#layout()} does that. Calling {@code layout()} from here would run the whole
   * cascade mid-construction, before subclass state is complete.
   */
  public void setItems(List<String> newItems) {
    Objects.requireNonNull(newItems, "newItems must not be null");
    items = new ArrayList<>(newItems);
    selectedIndex = NO_SELECTION;
    scrollOffsetY = 0f;
    hoveredIndex = NO_SELECTION;
    buildItemLabels();
    recalcMetrics();
  }

  /** The selected row index, or {@link #NO_SELECTION}. */
  public int getSelectedIndex() {
    return selectedIndex;
  }

  /**
   * Selects a row without emitting {@link #onItemSelected}. Pass {@link #NO_SELECTION} to clear.
   *
   * @throws IndexOutOfBoundsException if {@code index} is neither a valid row nor {@link
   *     #NO_SELECTION}
   */
  public void setSelectedIndex(int index) {
    if (index != NO_SELECTION && (index < 0 || index >= items.size())) {
      throw new IndexOutOfBoundsException(
          "index " + index + " is out of range for " + items.size() + " items");
    }
    selectedIndex = index;
  }

  public String getSelectedItem() {
    return (selectedIndex >= 0 && selectedIndex < items.size()) ? items.get(selectedIndex) : null;
  }

  /** Scrolls so the bottom of the content is visible — the usual want for an append-only log. */
  public void scrollToEnd() {
    scrollOffsetY = maxScrollY;
  }

  // -------------------------------------------------------------------------
  // LayoutElement
  // -------------------------------------------------------------------------

  /**
   * Natural size: the widest row by the full content height. A log viewer is normally given a grow
   * weight instead, since its natural height grows without bound as entries accumulate.
   */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    float widest = 0f;
    for (TextLabel label : itemLabels) {
      widest = Math.max(widest, label.getMeasuredWidth());
    }
    float rowH = UiHelper.barHeight(skin.font(), ITEM_PADDING_V);
    return out.set(widest + ITEM_PADDING_H * 2f, rowH * Math.max(1, items.size()));
  }

  // -------------------------------------------------------------------------
  // Clippable
  // -------------------------------------------------------------------------

  @Override
  public void setClipContext(Viewport viewport, Camera camera) {
    scissor.setClipContext(viewport, camera);
  }

  // -------------------------------------------------------------------------
  // CompositeWidget template methods
  // -------------------------------------------------------------------------

  /**
   * Sizes the chrome to the frame and recomputes row height / scroll range. Label positions are
   * resolved per-frame in {@link #doDraw(PolygonSpriteBatch)} to account for the scroll offset.
   */
  @Override
  protected void doBoundedLayout() {
    bounds.set(frame);
    border.setBounds(frame.x, frame.y, frame.width, frame.height);
    bg.setBounds(frame.x, frame.y, frame.width, frame.height);
    scissor.setBounds(frame.x, frame.y, frame.width, frame.height);
    recalcMetrics();
  }

  /**
   * Draws the background, selection highlight, and item rows. Rows are clipped; the background and
   * border are not, so the border's own edges stay crisp.
   */
  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    bg.draw(batch);

    float topY = frame.y + frame.height;

    // Position rows for the current scroll offset and draw the selection highlight.
    for (int i = 0; i < itemLabels.size(); i++) {
      float itemBottom = topY - (i + 1) * itemHeight + scrollOffsetY;
      itemLabels.get(i).setBoxAlign(Align.left | Align.center);
      itemLabels
          .get(i)
          .setBounds(
              frame.x + ITEM_PADDING_H,
              itemBottom,
              Math.max(0f, frame.width - ITEM_PADDING_H * 2f),
              itemHeight);

      if (i == selectedIndex && itemBottom + itemHeight >= frame.y && itemBottom <= topY) {
        batch.setColor(COLOR_SELECTED_BG);
        batch.draw(pixel, frame.x, itemBottom, frame.width, itemHeight);
        batch.setColor(Color.WHITE);
      }
    }

    if (!scissor.begin(batch)) return;

    for (int i = 0; i < itemLabels.size(); i++) {
      // Cull rows entirely outside the visible area.
      float itemBottom = topY - (i + 1) * itemHeight + scrollOffsetY;
      if (itemBottom + itemHeight < frame.y || itemBottom > topY) continue;

      Color textColor =
          (i == hoveredIndex && i != selectedIndex) ? COLOR_ITEM_HOVER : COLOR_ITEM_NORMAL;
      itemLabels.get(i).drawWithColor(batch, textColor);
    }
  }

  /** Closes the clip region opened in {@link #doDraw}, then draws the border on top of it. */
  @Override
  protected void doAfterDraw(PolygonSpriteBatch batch) {
    scissor.end(batch);
    border.draw(batch); // outside the clip so all four edges are fully visible
  }

  @Override
  protected void doBoundedReset() {
    hoveredIndex = NO_SELECTION;
    scrollOffsetY = 0f;
  }

  // -------------------------------------------------------------------------
  // UiWidget overrides — intercept at composite level; do not delegate to children
  // -------------------------------------------------------------------------

  @Override
  public void updateHover(float worldX, float worldY) {
    hovered = bounds.contains(worldX, worldY);
    hoveredIndex = hovered ? rowAt(worldY) : NO_SELECTION;
  }

  @Override
  public boolean hit(float worldX, float worldY) {
    if (!bounds.contains(worldX, worldY)) return false;
    int idx = rowAt(worldY);
    if (idx != NO_SELECTION) {
      selectedIndex = idx;
      onItemSelected.emit(selectedIndex);
    }
    return true;
  }

  @Override
  public boolean scrolled(float worldX, float worldY, float amountX, float amountY) {
    if (!bounds.contains(worldX, worldY)) return false;
    if (itemHeight <= 0f) return true;
    scrollOffsetY =
        MathUtils.clamp(scrollOffsetY + amountY * itemHeight * SCROLL_SPEED, 0f, maxScrollY);
    return true;
  }

  @Override
  public boolean update(float delta) {
    return false;
  }

  // -------------------------------------------------------------------------
  // Internal
  // -------------------------------------------------------------------------

  /** Row index under {@code worldY}, or {@link #NO_SELECTION}. */
  private int rowAt(float worldY) {
    return itemIndexAt(worldY, frame.y, frame.height, itemHeight, scrollOffsetY, items.size());
  }

  /**
   * The row index at world Y {@code worldY} within a list of {@code count} rows of {@code
   * itemHeight} each, scrolled by {@code scrollOffset}.
   *
   * <p>Extracted as a pure static so the row arithmetic is unit-testable without GL. Returns {@link
   * #NO_SELECTION} for a point outside the populated rows <em>and</em> when {@code itemHeight} is
   * not positive — the latter happens whenever input arrives before the first layout pass, which
   * would otherwise divide by zero.
   */
  public static int itemIndexAt(
      float worldY, float listY, float listH, float itemHeight, float scrollOffset, int count) {
    if (itemHeight <= 0f || count <= 0) return NO_SELECTION;
    float relY = (listY + listH) - worldY + scrollOffset;
    if (relY < 0f) return NO_SELECTION;
    int idx = (int) (relY / itemHeight);
    return (idx >= 0 && idx < count) ? idx : NO_SELECTION;
  }

  private void buildItemLabels() {
    itemLabels = new ArrayList<>(items.size());
    for (String item : items) {
      // Bounds are assigned per-frame in doDraw() to follow the scroll offset.
      itemLabels.add(new TextLabel(skin.font(), item));
    }
  }

  private void recalcMetrics() {
    itemHeight = UiHelper.barHeight(skin.font(), ITEM_PADDING_V);
    float totalContentH = items.size() * itemHeight;
    maxScrollY = Math.max(0f, totalContentH - frame.height);
    scrollOffsetY = MathUtils.clamp(scrollOffsetY, 0f, maxScrollY);
  }
}
