package com.cryptroot.core.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Maps a world-space rectangle to a GL scissor region for batch-clipped drawing.
 *
 * <p>Call {@link #begin(PolygonSpriteBatch)} to flush the batch and enable the scissor test; call
 * {@link #end(PolygonSpriteBatch)} to flush and disable it. Calls must always be paired — never
 * nest two open regions simultaneously.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * ScissorRegion scissor = new ScissorRegion(viewport, cx, cy, cw, ch);
 *
 * // In doBoundedLayout():
 * scissor.setBounds(contentX, contentY, contentW, contentH);
 *
 * // In doDraw / doAfterDraw:
 * scissor.begin(batch);
 * // ... draw clipped content ...
 * scissor.end(batch);
 * }</pre>
 */
public final class ScissorRegion {

  private final Viewport viewport;
  private float x;
  private float y;
  private float w;
  private float h;

  /**
   * @param viewport the scene viewport used for world-to-screen coordinate mapping
   * @param x left edge of the clip region in world coordinates
   * @param y bottom edge of the clip region in world coordinates
   * @param w width in world coordinates
   * @param h height in world coordinates
   */
  public ScissorRegion(Viewport viewport, float x, float y, float w, float h) {
    this.viewport = viewport;
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  /** Updates the clip rectangle in world coordinates. */
  public void setBounds(float x, float y, float w, float h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  /**
   * Flushes {@code batch} and enables the GL scissor test over this region. Must be followed by a
   * matching call to {@link #end(PolygonSpriteBatch)}.
   */
  public void begin(PolygonSpriteBatch batch) {
    batch.flush();
    float scaleX = (float) viewport.getScreenWidth() / viewport.getWorldWidth();
    float scaleY = (float) viewport.getScreenHeight() / viewport.getWorldHeight();
    int sx = viewport.getScreenX() + MathUtils.round(x * scaleX);
    int sy = viewport.getScreenY() + MathUtils.round(y * scaleY);
    int sw = MathUtils.round(w * scaleX);
    int sh = MathUtils.round(h * scaleY);
    Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
    Gdx.gl.glScissor(sx, sy, sw, sh);
  }

  /**
   * Flushes {@code batch} and disables the GL scissor test. Must be called after a matching {@link
   * #begin(PolygonSpriteBatch)}.
   */
  public void end(PolygonSpriteBatch batch) {
    batch.flush();
    Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
  }
}
