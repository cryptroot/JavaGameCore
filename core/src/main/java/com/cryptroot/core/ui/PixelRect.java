package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import java.util.Objects;

/**
 * A non-interactive widget that draws a solid colour-filled rectangle.
 *
 * <p>This is the fill counterpart to {@link PixelBorder}: where {@code PixelBorder} outlines a
 * rect, {@code PixelRect} fills it. The two are designed to be used together for widget
 * backgrounds.
 *
 * <p>The colour can be changed at any time via {@link #setColor(Color)}. Bounds are set via {@link
 * #setBounds(float, float, float, float)} or {@link #setPosition(float, float)} and take effect on
 * the next {@link #draw} call, so it is safe to call them from {@code doLayout()} before the layout
 * cascade.
 *
 * <h3>Usage — as a registered child (drawn first = behind everything)</h3>
 *
 * <pre>{@code
 * // In composite constructor (add before all other children):
 * bg = new PixelRect(pixel, COLOR_BG);
 * addChild(bg);
 *
 * // In doLayout():
 * bg.setBounds(x, y, w, h);
 * }</pre>
 *
 * <h3>Usage — as a plain field (for explicit draw ordering within doDraw)</h3>
 *
 * <pre>{@code
 * // In constructor:
 * bg = new PixelRect(pixel, COLOR_BG);
 *
 * // In layout() / doLayout():
 * bg.setBounds(x, y, w, h);
 *
 * // In draw() / doDraw(), as the very first draw call:
 * bg.draw(batch);
 * }</pre>
 */
public final class PixelRect implements UiWidget {

  private final Texture pixel;
  private final Color color;

  private float x;
  private float y;
  private float w;
  private float h;

  /**
   * @param pixel 1×1 white pixel texture for solid rect drawing
   * @param color fill colour (copied; caller may discard the source)
   */
  public PixelRect(Texture pixel, Color color) {
    Objects.requireNonNull(pixel, "pixel must not be null");
    Objects.requireNonNull(color, "color must not be null");
    this.pixel = pixel;
    this.color = color.cpy();
  }

  /**
   * Sets all four bounds at once. Intended to be called from {@code layout()} or a parent
   * composite's {@code doLayout()}.
   */
  public void setBounds(float x, float y, float w, float h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  /** Updates the fill colour (copied). */
  public void setColor(Color newColor) {
    Objects.requireNonNull(newColor, "newColor must not be null");
    color.set(newColor);
  }

  // -------------------------------------------------------------------------
  // UiWidget — non-interactive
  // -------------------------------------------------------------------------

  /** Repositions the rect without altering its width or height. */
  @Override
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public void layout() {}

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
   * Fills the bounds rect with the stored colour, then restores the batch colour to {@link
   * Color#WHITE}.
   */
  @Override
  public void draw(PolygonSpriteBatch batch) {
    batch.setColor(color);
    batch.draw(pixel, x, y, w, h);
    batch.setColor(Color.WHITE);
  }
}
