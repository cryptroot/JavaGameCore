package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;

/**
 * A non-interactive widget that draws a solid pixel-based rectangular border.
 *
 * <p>The border is rendered as four thin axis-aligned rectangles of a fixed {@link #thickness} that
 * together outline the bounds of the widget.
 *
 * <p>Bounds can be set at construction and updated at any time via {@link #setBounds(float, float,
 * float, float)}, which is intended to be called from a parent composite's {@code doLayout()} just
 * before the layout cascade. Direct {@link #draw(PolygonSpriteBatch)} calls are also fine when the
 * widget is used as a plain field rather than a registered child.
 *
 * <h3>Usage — as a registered child</h3>
 *
 * <pre>{@code
 * // In composite constructor:
 * border = new PixelBorder(pixel, 2f, new Color(1f, 1f, 1f, 0.8f));
 * addChild(border);
 *
 * // In doLayout():
 * border.setBounds(x, y, w, h);
 * }</pre>
 *
 * <h3>Usage — as a plain field (for explicit draw ordering)</h3>
 *
 * <pre>{@code
 * // In constructor:
 * border = new PixelBorder(pixel, 1f, Color.GRAY.cpy());
 *
 * // In layout():
 * border.setBounds(x, y, w, h);
 *
 * // In draw():
 * border.draw(batch);
 * }</pre>
 */
public final class PixelBorder implements UiWidget {

  private final Texture pixel;
  private final Color color;
  private final float thickness;

  private float x;
  private float y;
  private float w;
  private float h;

  /**
   * @param pixel 1×1 white pixel texture for solid rect drawing
   * @param thickness border stroke width in world units
   * @param color border colour (copied; caller may discard the source)
   */
  public PixelBorder(Texture pixel, float thickness, Color color) {
    this.pixel = pixel;
    this.thickness = thickness;
    this.color = color.cpy();
  }

  /**
   * Sets all four bounds at once. Intended to be called from a parent composite's {@code
   * doLayout()} so the bounds are up-to-date before the first {@link #draw} call.
   */
  public void setBounds(float x, float y, float w, float h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  // -------------------------------------------------------------------------
  // UiWidget — non-interactive
  // -------------------------------------------------------------------------

  /** Repositions the border without altering its width or height. */
  @Override
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  /** No remeasurement needed; bounds are set explicitly via {@link #setBounds}. */
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
   * Draws four thin axis-aligned rects that outline {@code (x, y, w, h)}. Bottom-left origin
   * convention: {@code (x, y)} is the bottom-left corner. Restores the batch colour to {@link
   * Color#WHITE} after drawing.
   */
  @Override
  public void draw(PolygonSpriteBatch batch) {
    float t = thickness;
    batch.setColor(color);
    batch.draw(pixel, x, y, w, t); // bottom
    batch.draw(pixel, x, y + h - t, w, t); // top
    batch.draw(pixel, x, y, t, h); // left
    batch.draw(pixel, x + w - t, y, t, h); // right
    batch.setColor(Color.WHITE);
  }
}
