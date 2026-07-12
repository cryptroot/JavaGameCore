package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import java.util.Objects;

/**
 * Non-interactive widget that draws a {@link Texture} scaled to fill given bounds.
 *
 * <p>Intended for background images (map tiles, splash art, etc.) that sit behind interactive
 * children. The texture is drawn with full white tint (no colour modulation) and the batch colour
 * is restored to {@link Color#WHITE} afterwards.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * TextureWidget bg = new TextureWidget(mapTexture, 0f, 70f, 1600f, 750f);
 * panel.addWidget(bg);   // add before interactive children so it draws behind them
 * }</pre>
 */
public final class TextureWidget implements UiWidget {

  private Texture texture;
  private float x;
  private float y;
  private float w;
  private float h;

  /**
   * @param texture the texture to draw (not disposed by this widget)
   * @param x left edge in world coordinates
   * @param y bottom edge in world coordinates
   * @param w draw width
   * @param h draw height
   */
  public TextureWidget(Texture texture, float x, float y, float w, float h) {
    Objects.requireNonNull(texture, "texture must not be null");
    this.texture = texture;
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  /** Repositions and resizes the widget without altering the texture reference. */
  public void setBounds(float x, float y, float w, float h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  /** Replaces the texture drawn by this widget. The old texture is not disposed. */
  public void setTexture(Texture texture) {
    Objects.requireNonNull(texture, "texture must not be null");
    this.texture = texture;
  }

  // -------------------------------------------------------------------------
  // UiWidget — non-interactive
  // -------------------------------------------------------------------------

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

  @Override
  public void draw(PolygonSpriteBatch batch) {
    batch.setColor(Color.WHITE);
    batch.draw(texture, x, y, w, h);
  }
}
