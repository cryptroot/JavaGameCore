package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.cryptroot.core.event.Signal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A vertical stack of clickable option rows used by {@link ConversationWidget} to present a {@link
 * com.cryptroot.core.dialogue.DialogueNode.Choice}.
 *
 * <p>Display-only chrome aside, the widget is interactive: a click on an enabled row emits {@link
 * #onSelect} with that row's index. Disabled rows render greyed and never emit. The list is hidden
 * until {@link #show()} is called and is hidden again by {@link #hide()} / {@link #reset()}.
 */
public final class ChoiceList implements UiWidget {

  // ---- Layout ----
  private static final float ROW_H = 48f;
  private static final float ROW_GAP = 8f;
  private static final float PAD_H = 18f;

  // ---- Colours ----
  private static final Color BG_COLOR = new Color(0.08f, 0.08f, 0.14f, 0.92f);
  private static final Color BG_HOVER = new Color(0.18f, 0.18f, 0.30f, 0.95f);
  private static final Color BG_DISABLED = new Color(0.06f, 0.06f, 0.08f, 0.85f);
  private static final Color BORDER_COLOR = new Color(0.55f, 0.55f, 0.75f, 1f);
  private static final Color TEXT_COLOR = Color.WHITE;
  private static final Color TEXT_DISABLED = new Color(0.50f, 0.50f, 0.55f, 1f);
  private static final float BORDER_W = 1f;

  private final Texture pixel;
  private final BitmapFont font;
  private final GlyphLayout layout = new GlyphLayout();

  private float ax, ay, aw, ah; // anchor area (bottom-left, width, height)

  private final List<String> labels = new ArrayList<>();
  private final List<Boolean> enabled = new ArrayList<>();
  private final List<Rectangle> rowRects = new ArrayList<>();

  private boolean visible = false;
  private int hoverIndex = -1;

  /** Fires the selected (enabled) option index. */
  public final Signal<Integer> onSelect = new Signal<>();

  public ChoiceList(Texture pixel, BitmapFont font) {
    Objects.requireNonNull(pixel, "pixel must not be null");
    Objects.requireNonNull(font, "font must not be null");
    this.pixel = pixel;
    this.font = font;
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /** Sets the anchor area; rows are laid out from the bottom upward. */
  public void setBounds(float x, float y, float w, float h) {
    this.ax = x;
    this.ay = y;
    this.aw = w;
    this.ah = h;
  }

  /** Replaces the displayed options. {@code enabledFlags} may be {@code null} (all enabled). */
  public void setOptions(List<String> optionLabels, boolean[] enabledFlags) {
    Objects.requireNonNull(optionLabels, "optionLabels must not be null");
    labels.clear();
    enabled.clear();
    for (int i = 0; i < optionLabels.size(); i++) {
      labels.add(optionLabels.get(i));
      enabled.add(enabledFlags == null || i >= enabledFlags.length || enabledFlags[i]);
    }
    layout();
  }

  public void show() {
    visible = true;
  }

  public void hide() {
    visible = false;
    hoverIndex = -1;
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  // -------------------------------------------------------------------------
  // UiWidget
  // -------------------------------------------------------------------------

  @Override
  public void layout() {
    rowRects.clear();
    int n = labels.size();
    // Stack rows from the bottom of the anchor area upward (last option lowest).
    for (int i = 0; i < n; i++) {
      float rowY = ay + i * (ROW_H + ROW_GAP);
      rowRects.add(new Rectangle(ax, rowY, aw, ROW_H));
    }
  }

  @Override
  public void updateHover(float worldX, float worldY) {
    hoverIndex = -1;
    if (!visible) return;
    for (int i = 0; i < rowRects.size(); i++) {
      if (rowRects.get(i).contains(worldX, worldY) && enabled.get(i)) {
        hoverIndex = i;
        return;
      }
    }
  }

  @Override
  public boolean hit(float worldX, float worldY) {
    if (!visible) return false;
    for (int i = 0; i < rowRects.size(); i++) {
      if (rowRects.get(i).contains(worldX, worldY)) {
        if (enabled.get(i)) {
          onSelect.emit(i);
        }
        return true; // consume clicks on any row (even disabled)
      }
    }
    return false;
  }

  @Override
  public boolean update(float delta) {
    return false;
  }

  @Override
  public void reset() {
    hide();
  }

  @Override
  public void draw(PolygonSpriteBatch batch) {
    if (!visible) return;
    for (int i = 0; i < rowRects.size(); i++) {
      Rectangle r = rowRects.get(i);
      boolean on = enabled.get(i);

      // Background
      batch.setColor(!on ? BG_DISABLED : (i == hoverIndex ? BG_HOVER : BG_COLOR));
      batch.draw(pixel, r.x, r.y, r.width, r.height);
      batch.setColor(Color.WHITE);

      // Border
      batch.setColor(BORDER_COLOR);
      batch.draw(pixel, r.x, r.y, r.width, BORDER_W);
      batch.draw(pixel, r.x, r.y + r.height - BORDER_W, r.width, BORDER_W);
      batch.draw(pixel, r.x, r.y, BORDER_W, r.height);
      batch.draw(pixel, r.x + r.width - BORDER_W, r.y, BORDER_W, r.height);
      batch.setColor(Color.WHITE);

      // Label (vertically centred)
      font.setColor(on ? TEXT_COLOR : TEXT_DISABLED);
      layout.setText(font, labels.get(i));
      float textY = r.y + (r.height + layout.height) / 2f;
      font.draw(batch, labels.get(i), r.x + PAD_H, textY, r.width - PAD_H * 2f, Align.left, false);
      font.setColor(Color.WHITE);
    }
  }
}
