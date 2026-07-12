package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import java.util.Objects;

/**
 * A lightweight fill-track renderer shared by {@link Slider} and {@link ProgressBar}.
 *
 * <p>Draws a background rect followed by a proportionally filled rect on top. The fill ratio (0–1)
 * is set via {@link #setFillRatio(float)}; geometry is configured via {@link #setBounds(float,
 * float, float, float)}.
 *
 * <p>This is a helper widget managed and drawn by its owning composite — it is not intended to be
 * added to a {@link UiLayer} directly.
 */
public final class FillTrack implements UiWidget {

  private final Texture pixel;
  private final Color bgColor;
  private final Color fillColor;

  private float x, y, w, h;
  private float fillRatio;

  /**
   * @param pixel 1×1 white texture for solid rect drawing
   * @param bgColor colour for the unfilled track background
   * @param fillColor colour for the filled portion
   */
  public FillTrack(Texture pixel, Color bgColor, Color fillColor) {
    Objects.requireNonNull(pixel, "pixel must not be null");
    Objects.requireNonNull(bgColor, "bgColor must not be null");
    Objects.requireNonNull(fillColor, "fillColor must not be null");
    this.pixel = pixel;
    this.bgColor = bgColor;
    this.fillColor = fillColor;
  }

  /** Sets the position and size of the track in world coordinates. */
  public void setBounds(float x, float y, float w, float h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  /** Sets the fill ratio. Value is clamped to [0, 1]. */
  public void setFillRatio(float ratio) {
    fillRatio = MathUtils.clamp(ratio, 0f, 1f);
  }

  // -------------------------------------------------------------------------
  // UiWidget — only draw() is meaningful; all others are no-ops
  // -------------------------------------------------------------------------

  @Override
  public void layout() {}

  @Override
  public void draw(PolygonSpriteBatch batch) {
    batch.setColor(bgColor);
    batch.draw(pixel, x, y, w, h);

    float fillW = w * fillRatio;
    if (fillW > 0f) {
      batch.setColor(fillColor);
      batch.draw(pixel, x, y, fillW, h);
    }

    batch.setColor(Color.WHITE);
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
}
