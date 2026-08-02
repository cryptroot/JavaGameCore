package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import java.util.Objects;

/**
 * A horizontal progress bar widget.
 *
 * <p>Draws a filled track and a centred percentage label. The fill extends from the left edge
 * proportional to {@link #setProgress(float)} (0–1).
 *
 * <p>Position and size can be updated after construction via {@link #setBounds(float, float, float,
 * float)}, which is designed to be called from a parent composite's {@code doLayout()} before the
 * layout cascade reaches this widget.
 *
 * <h3>Usage — standalone</h3>
 *
 * <pre>{@code
 * ProgressBar bar = new ProgressBar(pixel, game.getFontHint(),
 *     80f, 200f, 400f, 22f, 0.65f);
 * uiLayer.add(bar, 0);
 * }</pre>
 *
 * <h3>Usage — as a child composite</h3>
 *
 * <pre>{@code
 * // In parent constructor:
 * progressBar = new ProgressBar(pixel, hintFont, 0f, 0f, 0f, TRACK_H, 0f);
 * addChild(progressBar);
 *
 * // In parent doLayout():
 * progressBar.setBounds(trackX, trackY, trackW, TRACK_H);
 * }</pre>
 */
public final class ProgressBar extends BoundedWidget {

  private static final Color COLOR_TRACK_BG = new Color(0.22f, 0.22f, 0.32f, 1f);
  private static final Color COLOR_FILL = new Color(0.30f, 0.75f, 0.45f, 1f);

  /** Vertical padding used when reporting a natural height. */
  private static final float LABEL_PADDING_V = 6f;

  private final BitmapFont labelFont;
  private final FillTrack fillTrack;

  private float progress;

  private final TextLabel pctLabel;

  /**
   * @param pixel 1×1 white pixel texture for solid-rect drawing
   * @param labelFont font for the centred percentage label
   * @param x left edge of the bar in world coordinates
   * @param y bottom edge of the bar in world coordinates
   * @param w width of the bar
   * @param h height of the bar
   * @param progress initial fill ratio; clamped to [0, 1]
   */
  public ProgressBar(
      Texture pixel, BitmapFont labelFont, float x, float y, float w, float h, float progress) {
    Objects.requireNonNull(pixel, "pixel must not be null");
    Objects.requireNonNull(labelFont, "labelFont must not be null");
    this.labelFont = labelFont;
    this.progress = Math.max(0f, Math.min(1f, progress));
    setBounds(x, y, w, h);

    fillTrack = new FillTrack(pixel, COLOR_TRACK_BG, COLOR_FILL);
    pctLabel = new TextLabel(labelFont, pctText()).setBoxAlign(Align.center);
    addChild(pctLabel);
  }

  /** Creates a bar sized by its enclosing layout container. */
  public ProgressBar(Texture pixel, BitmapFont labelFont, float progress) {
    this(pixel, labelFont, 0f, 0f, 0f, 0f, progress);
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /** Sets the fill ratio. Value is clamped to [0, 1]. */
  public void setProgress(float progress) {
    this.progress = Math.max(0f, Math.min(1f, progress));
    pctLabel.setText(pctText());
  }

  public float getProgress() {
    return progress;
  }

  // -------------------------------------------------------------------------
  // BoundedWidget
  // -------------------------------------------------------------------------

  /** Natural size: wide enough for the percentage text, one standard text bar tall. */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    return out.set(
        pctLabel.getMeasuredWidth() + LABEL_PADDING_V * 4f,
        UiHelper.barHeight(labelFont, LABEL_PADDING_V));
  }

  @Override
  protected void doBoundedLayout() {
    bounds.set(frame);
    fillTrack.setBounds(frame.x, frame.y, frame.width, frame.height);
    // Centre the label in the bar via the shared metric helper rather than an inline cap-height
    // formula.
    pctLabel.setBounds(frame.x, frame.y, frame.width, frame.height);
  }

  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    fillTrack.setFillRatio(progress);
    fillTrack.draw(batch);
  }

  // -------------------------------------------------------------------------
  // Private
  // -------------------------------------------------------------------------

  private String pctText() {
    return (int) Math.round(progress * 100) + "%";
  }
}
