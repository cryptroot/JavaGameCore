package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cryptroot.core.event.Signal;
import java.util.ArrayList;
import java.util.List;

/**
 * A vertically scrollable list of text items, clipped to its declared bounds.
 *
 * <p>Each item string is backed by a {@link TextLabel} that handles glyph layout caching. Label
 * positions are updated every frame in {@link #doDraw(PolygonSpriteBatch)} to reflect the current
 * scroll offset, so the cached {@link com.badlogic.gdx.graphics.g2d.GlyphLayout} is only remeasured
 * when an item's text or position actually changes.
 *
 * <p>Items can be scrolled with the mouse wheel. Clicking an item selects it and emits {@link
 * #onItemSelected}:
 *
 * <pre>{@code
 * ScrollList list = new ScrollList(
 *     skin, pixel, viewport,
 *     900f, 200f, 360f, 300f,
 *     List.of("Alpha", "Beta", "Gamma"));
 * list.onItemSelected.connect(idx -> status = "Selected: " + items.get(idx));
 * uiLayer.add(list, 0);
 * }</pre>
 *
 * <p>Items can be replaced at any time via {@link #setItems(List)}.
 *
 * <p><b>Scissor clipping:</b> {@link #doDraw(PolygonSpriteBatch)} flushes and enables a GL scissor
 * before drawing item labels; {@link #doAfterDraw(PolygonSpriteBatch)} flushes and disables it.
 * This is transparent to the owning screen.
 *
 * <p><b>Labels as helpers, not children:</b> the {@link TextLabel} objects are held in a private
 * typed list and drawn manually inside the scissor region. They are <em>not</em> registered as
 * {@link CompositeWidget} children, because their rendering must be wrapped by GL scissor state
 * that cannot be expressed in the standard before-children / after-children template split.
 */
public final class ScrollList extends CompositeWidget {

  private static final float ITEM_PADDING_H = 10f;
  private static final float ITEM_PADDING_V = 6f;
  private static final float SCROLL_SPEED = 1f;

  private static final Color COLOR_ITEM_NORMAL = Color.WHITE;
  private static final Color COLOR_ITEM_HOVER = new Color(0.85f, 0.85f, 1.0f, 1f);
  private static final Color COLOR_SELECTED_BG = new Color(0.25f, 0.25f, 0.45f, 1f);

  /** Fires with the index of the item that was just clicked. */
  public final Signal<Integer> onItemSelected = new Signal<>();

  private final UiSkin skin;
  private final Texture pixel;
  private final ScissorRegion scissor;

  private final float listX;
  private final float listY;
  private final float listW;
  private final float listH;

  private final Rectangle bounds = new Rectangle();
  private final PixelBorder border;
  private final PixelRect bg;

  private List<String> items = new ArrayList<>();
  private List<TextLabel> itemLabels = new ArrayList<>();

  private int selectedIndex = -1;
  private float scrollOffsetY = 0f;
  private float itemHeight;
  private float maxScrollY;
  private int hoveredIndex = -1;

  /**
   * @param skin skin providing the font for item labels
   * @param pixel 1×1 white pixel texture for backgrounds and borders
   * @param viewport the scene's {@link Viewport} (used for scissor mapping)
   * @param listX left edge of the list in world coordinates
   * @param listY bottom edge of the list in world coordinates
   * @param listW width of the list in world coordinates
   * @param listH height of the list in world coordinates
   * @param items initial item list (copied)
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
    this.skin = skin;
    this.pixel = pixel;
    this.listX = listX;
    this.listY = listY;
    this.listW = listW;
    this.listH = listH;
    border = new PixelBorder(pixel, 2f, new Color(0.5f, 0.5f, 0.5f, 1f));
    bg = new PixelRect(pixel, new Color(0.10f, 0.10f, 0.16f, 1f));
    scissor = new ScissorRegion(viewport, listX, listY, listW, listH);
    setItems(items);
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Replaces the item list. Resets scroll position and selection, rebuilds the internal {@link
   * TextLabel} list, and performs a full layout pass.
   */
  public void setItems(List<String> newItems) {
    items = new ArrayList<>(newItems);
    selectedIndex = items.isEmpty() ? -1 : 0;
    scrollOffsetY = 0f;
    hoveredIndex = -1;
    buildItemLabels();
    layout();
  }

  public int getSelectedIndex() {
    return selectedIndex;
  }

  public String getSelectedItem() {
    return (selectedIndex >= 0 && selectedIndex < items.size()) ? items.get(selectedIndex) : null;
  }

  // -------------------------------------------------------------------------
  // CompositeWidget template methods
  // -------------------------------------------------------------------------

  /**
   * Resets {@link #bounds} and recomputes {@link #itemHeight} / {@link #maxScrollY} from the
   * current font metrics and item count. Label positions are resolved per-frame in {@link
   * #doDraw(PolygonSpriteBatch)} to account for scroll offset.
   */
  @Override
  protected void doLayout() {
    bounds.set(listX, listY, listW, listH);
    border.setBounds(listX, listY, listW, listH);
    bg.setBounds(listX, listY, listW, listH);
    recalcHeight();
  }

  /**
   * Draws the background, border, and item rows. For each item the label position is updated for
   * the current scroll offset (marks the {@link TextLabel} dirty only when the position actually
   * changed). Selection highlights are drawn before the scissor is opened; item labels are drawn
   * inside the scissor region using {@link TextLabel#drawWithColor(PolygonSpriteBatch, Color)} so
   * their hover/selected colour can be applied without allocating a {@link Color} per frame.
   */
  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    bg.draw(batch);

    float topY = listY + listH;

    // Update label positions for current scroll and draw selection highlights.
    for (int i = 0; i < itemLabels.size(); i++) {
      float itemBottom = topY - (i + 1) * itemHeight + scrollOffsetY;
      float textY = itemBottom + ITEM_PADDING_V + skin.font().getCapHeight();
      itemLabels.get(i).setPosition(listX + ITEM_PADDING_H, textY);

      // Selection highlight drawn outside scissor — no clipping needed for a full-width rect.
      if (i == selectedIndex && itemBottom + itemHeight >= listY && itemBottom <= listY + listH) {
        batch.setColor(COLOR_SELECTED_BG);
        batch.draw(pixel, listX, itemBottom, listW, itemHeight);
        batch.setColor(Color.WHITE);
      }
    }

    scissor.begin(batch);

    // Draw item labels inside the scissor region.
    for (int i = 0; i < itemLabels.size(); i++) {
      // Cull items entirely outside the visible area.
      float itemBottom = topY - (i + 1) * itemHeight + scrollOffsetY;
      if (itemBottom + itemHeight < listY || itemBottom > listY + listH) continue;

      Color textColor =
          (i == hoveredIndex && i != selectedIndex) ? COLOR_ITEM_HOVER : COLOR_ITEM_NORMAL;
      itemLabels.get(i).drawWithColor(batch, textColor);
    }
  }

  /** Flushes and disables the GL scissor test opened in {@link #doDraw}. */
  @Override
  protected void doAfterDraw(PolygonSpriteBatch batch) {
    scissor.end(batch);
    border.draw(batch); // drawn outside scissor so all edges are fully visible
  }

  @Override
  protected void doReset() {
    hoveredIndex = -1;
    scrollOffsetY = 0f;
  }

  // -------------------------------------------------------------------------
  // UiWidget overrides — intercept at composite level; do not delegate to children
  // -------------------------------------------------------------------------

  @Override
  public void updateHover(float worldX, float worldY) {
    if (!bounds.contains(worldX, worldY)) {
      hoveredIndex = -1;
      return;
    }
    float relY = (listY + listH) - worldY + scrollOffsetY;
    int idx = (int) (relY / itemHeight);
    hoveredIndex = (idx >= 0 && idx < items.size()) ? idx : -1;
  }

  @Override
  public boolean hit(float worldX, float worldY) {
    if (!bounds.contains(worldX, worldY)) return false;
    float relY = (listY + listH) - worldY + scrollOffsetY;
    int idx = (int) (relY / itemHeight);
    if (idx >= 0 && idx < items.size()) {
      selectedIndex = idx;
      onItemSelected.emit(selectedIndex);
    }
    return true;
  }

  @Override
  public boolean scrolled(float worldX, float worldY, float amountX, float amountY) {
    if (!bounds.contains(worldX, worldY)) return false;
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

  private void buildItemLabels() {
    itemLabels = new ArrayList<>(items.size());
    for (String item : items) {
      // Positions are placeholder (0, 0) until doDraw() sets them each frame.
      itemLabels.add(new TextLabel(skin.font(), item, 0f, 0f));
    }
  }

  private void recalcHeight() {
    itemHeight = UiHelper.barHeight(skin.font(), ITEM_PADDING_V);
    float totalContentH = items.size() * itemHeight;
    maxScrollY = Math.max(0f, totalContentH - listH);
  }
}
