package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.event.Signal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A generic typed combo-box widget.
 *
 * <p>The collapsed view is a fixed-width button showing the selected item's label followed by "▾".
 * Clicking opens an internal {@link PopupMenu} list anchored at the button's bottom edge; selecting
 * a row closes the list and emits {@link #onSelected}.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * Dropdown<WalkRoute> dd = new Dropdown<>(skin, pixel, ox, y, 360f);
 * dd.setItems(Arrays.asList(WalkRoute.values()),
 *             r -> r.displayName() + " (" + r.durationMinutes() + "min)");
 * dd.setSelected(WalkRoute.SHORT);
 * dd.onSelected.connect(route -> selectedRoute = route);
 * somePanel.addWidget(dd);
 * }</pre>
 *
 * <h3>Layout</h3>
 *
 * Width is fixed at construction; height is computed from the skin font's cap height plus vertical
 * padding (same formula as {@link Button}).
 *
 * <h3>Hit delegation</h3>
 *
 * {@link BoundedWidget#hit} only tests the widget's own bounds and does not propagate to children.
 * {@code Dropdown} overrides {@code hit} to first delegate to the internal {@link PopupMenu} when
 * it is open, then falls through to the button area for toggling.
 */
public final class Dropdown<T> extends BoundedWidget {

  // ── Visual constants ──────────────────────────────────────────────────────

  private static final float HOVER_DARKEN = 0.65f;
  private static final float PAD_H = 16f;
  private static final float PAD_V_BOT = 8f;
  private static final float PAD_V_TOP = 16f;
  private static final Color LABEL_COLOR = new Color(0.12f, 0.12f, 0.12f, 1f);

  // ── State ─────────────────────────────────────────────────────────────────

  /** Fires with the newly selected item after every user-driven selection change. */
  public final Signal<T> onSelected = new Signal<>();

  private final UiSkin skin;
  private final Texture pixel;
  private final float fieldX;
  private final float fieldY;
  private final float fieldW;

  private List<T> items = new ArrayList<>();
  private Function<T, String> labeller = Object::toString;
  private T selected;

  private final TextLabel labelWidget;
  private PopupMenu popup; // rebuilt by setItems; always a child

  // ── Constructor ───────────────────────────────────────────────────────────

  /**
   * @param skin skin supplying the font and nine-patch button slices
   * @param pixel 1×1 white texture passed to the internal {@link PopupMenu}
   * @param x left edge in world coordinates
   * @param y bottom edge (text baseline row) in world coordinates
   * @param w fixed width of the collapsed button
   */
  public Dropdown(UiSkin skin, Texture pixel, float x, float y, float w) {
    Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(pixel, "pixel must not be null");
    this.skin = skin;
    this.pixel = pixel;
    this.fieldX = x;
    this.fieldY = y;
    this.fieldW = w;

    labelWidget = new TextLabel(skin.font(), "— ▾", x + PAD_H, 0f, LABEL_COLOR);
    addChild(labelWidget);

    popup = new PopupMenu(pixel, skin.font());
    addChild(popup);
  }

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Replaces the item list and labeller. Rebuilds the internal popup; selects the first item
   * automatically when the list is non-empty. Does not emit {@link #onSelected}.
   */
  public void setItems(List<T> newItems, Function<T, String> newLabeller) {
    Objects.requireNonNull(newItems, "newItems must not be null");
    Objects.requireNonNull(newLabeller, "newLabeller must not be null");
    this.items = new ArrayList<>(newItems);
    this.labeller = newLabeller;
    this.selected = newItems.isEmpty() ? null : newItems.get(0);
    rebuildPopup();
    updateLabel();
  }

  /**
   * Programmatically selects {@code item} without emitting {@link #onSelected}. Updates the button
   * label immediately.
   */
  public void setSelected(T item) {
    this.selected = item;
    updateLabel();
  }

  /** Returns the currently selected item, or {@code null} when no items are set. */
  public T selected() {
    return selected;
  }

  // ── BoundedWidget ─────────────────────────────────────────────────────────

  @Override
  protected void doBoundedLayout() {
    float h = skin.font().getCapHeight() + PAD_V_BOT + PAD_V_TOP;
    bounds.set(fieldX, fieldY, fieldW, h);
    float labelY = fieldY + PAD_V_BOT + skin.font().getCapHeight();
    labelWidget.setPosition(fieldX + PAD_H, labelY);
  }

  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    if (popup.isVisible()) {
      skin.selectedSlice().draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);
    } else if (hovered) {
      batch.setColor(HOVER_DARKEN, HOVER_DARKEN, HOVER_DARKEN, 1f);
      skin.normalSlice().draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);
      batch.setColor(Color.WHITE);
    } else {
      skin.normalSlice().draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);
    }
  }

  @Override
  public void updateHover(float worldX, float worldY) {
    popup.updateHover(worldX, worldY);
    hovered = !popup.isVisible() && bounds.contains(worldX, worldY);
  }

  /**
   * When the popup is open, delegates to it first so row selection and outside-click dismissal are
   * handled by {@link PopupMenu#hit}. Clicking the button while the popup is closed opens it;
   * clicking it while open lets the popup self-dismiss without reopening.
   */
  @Override
  public boolean hit(float worldX, float worldY) {
    boolean wasOpen = popup.isVisible();
    if (wasOpen && popup.hit(worldX, worldY)) return true;
    if (!bounds.contains(worldX, worldY)) return false;
    if (!wasOpen) popup.show(bounds.x, bounds.y);
    return true;
  }

  @Override
  public boolean update(float delta) {
    return false;
  }

  @Override
  protected void doBoundedReset() {
    popup.hide();
  }

  // ── Internal ─────────────────────────────────────────────────────────────

  private void rebuildPopup() {
    removeChild(popup);
    popup = new PopupMenu(pixel, skin.font());
    for (T item : items) {
      popup.addEntry(
          labeller.apply(item),
          () -> {
            selected = item;
            updateLabel();
            onSelected.emit(item);
          });
    }
    addChild(popup);
  }

  private void updateLabel() {
    String text = (selected != null ? labeller.apply(selected) : "—") + " ▾";
    labelWidget.setText(text);
  }
}
