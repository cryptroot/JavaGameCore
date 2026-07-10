package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;

/**
 * A self-contained, display-only text widget.
 *
 * <p>{@code TextLabel} owns a {@link GlyphLayout} that is computed once per text / position /
 * alignment change (dirty-flag pattern) and reused every frame, eliminating the per-frame {@code
 * new GlyphLayout()} allocations that would otherwise appear in every composite widget's {@code
 * draw()} method.
 *
 * <h3>Alignment</h3>
 *
 * <ul>
 *   <li>{@link HAlign#LEFT} (default) — text baseline starts at {@code (x, y)}.
 *   <li>{@link HAlign#CENTER} with {@code targetWidth == 0} — text is centred horizontally
 *       <em>around</em> {@code x} (useful when {@code x} is a knob or icon centre). Draw X = {@code
 *       x − width/2}.
 *   <li>{@link HAlign#CENTER} with {@code targetWidth > 0} — text is centred <em>within</em> the
 *       region {@code [x, x + targetWidth]}. Draw X = {@code x + (targetWidth − width) / 2}.
 * </ul>
 *
 * <h3>Usage — standalone</h3>
 *
 * <pre>{@code
 * TextLabel versionLabel = new TextLabel(game.getFontHint(), "v0.1", 80f, 30f);
 * uiLayer.add(versionLabel, 0);
 * }</pre>
 *
 * <h3>Usage — inside a CompositeWidget</h3>
 *
 * <pre>{@code
 * // In the composite's constructor:
 * titleLabel = new TextLabel(font, "Quest Title", 0f, 0f);
 * addChild(titleLabel);
 *
 * // In doLayout():
 * titleLabel.setPosition(computedX, computedY);
 * // CompositeWidget.layout() calls titleLabel.layout() automatically after doLayout().
 * }</pre>
 */
public final class TextLabel implements UiWidget {

  /** Horizontal alignment mode for the label text. */
  public enum HAlign {
    LEFT,
    CENTER
  }

  private final BitmapFont font;
  private String text;
  private Color color; // always a private copy — never the source constant
  private float x;
  private float y; // BitmapFont baseline Y
  private HAlign align = HAlign.LEFT;
  private float targetWidth = 0f;

  // Cached layout — recomputed only when dirty.
  private final GlyphLayout glyphLayout = new GlyphLayout();
  private boolean dirty = true;
  private float drawX; // resolved draw X after alignment

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public TextLabel(BitmapFont font, String text, float x, float y) {
    this.font = font;
    this.text = text;
    this.x = x;
    this.y = y;
    this.color = Color.WHITE.cpy();
  }

  public TextLabel(BitmapFont font, String text, float x, float y, Color color) {
    this(font, text, x, y);
    this.color = color.cpy();
  }

  // -------------------------------------------------------------------------
  // Fluent setters
  // -------------------------------------------------------------------------

  /** Updates the displayed text. Marks the layout dirty. Returns {@code this} for chaining. */
  public TextLabel setText(String newText) {
    if (!newText.equals(text)) {
      text = newText;
      dirty = true;
    }
    return this;
  }

  /** Returns the current text string. */
  public String getText() {
    return text;
  }

  /** Sets the draw colour (copied). Returns {@code this} for chaining. */
  public TextLabel setColor(Color newColor) {
    color = newColor.cpy();
    dirty = true;
    return this;
  }

  /**
   * Configures alignment. Returns {@code this} for chaining.
   *
   * @param align {@link HAlign#LEFT} or {@link HAlign#CENTER}
   * @param targetWidth width of the region to centre within; pass {@code 0} to centre the text
   *     around {@code x} instead
   */
  public TextLabel setAlign(HAlign align, float targetWidth) {
    this.align = align;
    this.targetWidth = targetWidth;
    dirty = true;
    return this;
  }

  // -------------------------------------------------------------------------
  // Measured dimensions — available after layout() or draw()
  // -------------------------------------------------------------------------

  /** Width of the rendered text in world units. Forces a remeasure if dirty. */
  public float getMeasuredWidth() {
    if (dirty) remeasure();
    return glyphLayout.width;
  }

  /** Height of the rendered text in world units. Forces a remeasure if dirty. */
  public float getMeasuredHeight() {
    if (dirty) remeasure();
    return glyphLayout.height;
  }

  /** Resolved draw X after alignment is applied. Forces a remeasure if dirty. */
  public float getDrawX() {
    if (dirty) remeasure();
    return drawX;
  }

  // -------------------------------------------------------------------------
  // UiWidget — non-interactive
  // -------------------------------------------------------------------------

  /**
   * Repositions the label and marks layout dirty. The next call to {@link #layout()} (or
   * automatically on first {@link #draw()}) will recompute the draw X.
   */
  @Override
  public void setPosition(float newX, float newY) {
    if (this.x != newX || this.y != newY) {
      this.x = newX;
      this.y = newY;
      dirty = true;
    }
  }

  /** Forces an immediate remeasure and caches the resolved draw X. */
  @Override
  public void layout() {
    remeasure();
  }

  @Override
  public void updateHover(float worldX, float worldY) {}

  @Override
  public boolean hit(float worldX, float worldY) {
    return false;
  }

  @Override
  public boolean update(float delta) {
    return false;
  }

  @Override
  public void reset() {}

  /**
   * Draws the text using the cached {@link GlyphLayout}. Remeasures lazily if the layout is dirty.
   * Always restores the font colour to {@link Color#WHITE} after drawing.
   */
  @Override
  public void draw(PolygonSpriteBatch batch) {
    font.setColor(color);
    if (dirty) remeasure();
    font.draw(batch, glyphLayout, drawX, y);
    font.setColor(Color.WHITE);
  }

  /**
   * Draws the text using the supplied {@code colorOverride} instead of the stored colour. The
   * colour is applied directly with no copy and no side effect on the stored colour. The font
   * colour is restored to {@link Color#WHITE} after drawing.
   *
   * <p>This overload is intended for widgets (e.g. {@link ScrollList}) that need to vary per-item
   * colour (hover / selected) without allocating a new {@link Color} every frame.
   */
  public void drawWithColor(PolygonSpriteBatch batch, Color colorOverride) {
    if (dirty) remeasure(); // ensure drawX reflects current position
    font.setColor(colorOverride);
    glyphLayout.setText(font, text); // re-bake layout with the override colour
    font.draw(batch, glyphLayout, drawX, y);
    font.setColor(Color.WHITE);
    dirty = true; // force re-bake with stored colour on next draw()
  }

  /**
   * Like {@link #drawWithColor} but draws at {@code (drawX + dxOffset, y + dyOffset)}. Does
   * <em>not</em> set the dirty flag — intended for a preceding shadow pass followed immediately by
   * a call to {@link #drawWithColor}.
   */
  public void drawWithColorOffset(
      PolygonSpriteBatch batch, Color colorOverride, float dxOffset, float dyOffset) {
    if (dirty) remeasure();
    font.setColor(colorOverride);
    glyphLayout.setText(font, text);
    font.draw(batch, glyphLayout, drawX + dxOffset, y + dyOffset);
    font.setColor(Color.WHITE);
  }

  // -------------------------------------------------------------------------
  // Private
  // -------------------------------------------------------------------------

  private void remeasure() {
    font.setColor(color);
    glyphLayout.setText(font, text);
    font.setColor(Color.WHITE);
    drawX =
        switch (align) {
          case CENTER ->
              targetWidth > 0f
                  ? x + (targetWidth - glyphLayout.width) / 2f
                  : x - glyphLayout.width / 2f;
          default -> x;
        };
    dirty = false;
  }
}
