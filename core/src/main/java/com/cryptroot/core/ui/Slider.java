package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.cryptroot.core.event.Signal;

/**
 * A horizontal drag-to-change-value slider.
 *
 * <p>The slider draws a track and a knob on top. The knob is a solid pixel rect. When a proper knob
 * texture is ready, replace the pixel-rect draw call in {@link #draw(PolygonSpriteBatch)} with a
 * texture draw; the rest of the interaction code does not need to change.
 *
 * <p>Dragging or clicking anywhere on the track maps the pointer X to [{@code min}, {@code max}]
 * and emits {@link #onChanged}:
 *
 * <pre>{@code
 * Slider speed = new Slider(pixel, font, 600f, 600f, 400f, 0f, 100f, 50f);
 * speed.onChanged.connect(v -> speedLabel = String.format("%.0f", v));
 * uiLayer.add(speed, 0);
 * }</pre>
 */
public final class Slider extends BoundedWidget {

  private static final float TRACK_HEIGHT = 6f;
  private static final float KNOB_WIDTH = 14f;
  private static final float KNOB_HEIGHT = 28f;
  private static final float HIT_EXTRA = 12f; // extra hit area above/below the track

  private static final Color COLOR_TRACK_BG = new Color(0.35f, 0.35f, 0.35f, 1f);
  private static final Color COLOR_TRACK_FILL = new Color(0.55f, 0.55f, 0.9f, 1f);
  private static final Color COLOR_KNOB = new Color(0.85f, 0.85f, 1.0f, 1f);
  private static final Color COLOR_KNOB_HOVER = Color.WHITE;

  /** Fires whenever the value changes, carrying the new value. */
  public final Signal<Float> onChanged = new Signal<>();

  private final Texture pixel;
  private final TextLabel valueLabelText;
  private final FillTrack fillTrack;

  private final float trackX; // left edge of track
  private final float trackY; // Y centre of track
  private final float trackW; // total track width
  private final float min;
  private final float max;

  private float value;

  /**
   * @param pixel 1×1 white texture for all solid-rect drawing
   * @param font font used to draw the value label below the slider
   * @param trackX left edge of the track in world coordinates
   * @param trackY Y centre of the track in world coordinates
   * @param trackW width of the track in world coordinates
   * @param min minimum value (inclusive)
   * @param max maximum value (inclusive); must be &gt; {@code min}
   * @param initial starting value; clamped to [{@code min}, {@code max}]
   */
  public Slider(
      Texture pixel,
      BitmapFont font,
      float trackX,
      float trackY,
      float trackW,
      float min,
      float max,
      float initial) {
    this.pixel = pixel;
    this.trackX = trackX;
    this.trackY = trackY;
    this.trackW = trackW;
    this.min = min;
    this.max = max;
    // Label is centred around the knob x (targetWidth=0 = "centre around x" mode).
    valueLabelText = new TextLabel(font, "", 0f, 0f).setAlign(TextLabel.HAlign.CENTER, 0f);
    fillTrack = new FillTrack(pixel, COLOR_TRACK_BG, COLOR_TRACK_FILL);
    setValue(MathUtils.clamp(initial, min, max));
  }

  public float getValue() {
    return value;
  }

  /** Sets the value, clamping to [{@code min}, {@code max}]. Does not emit {@link #onChanged}. */
  public void setValue(float newValue) {
    value = MathUtils.clamp(newValue, min, max);
    syncLabelWidget();
  }

  // -------------------------------------------------------------------------
  // BoundedWidget template methods
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    bounds.set(
        trackX - KNOB_WIDTH / 2f,
        trackY - KNOB_HEIGHT / 2f - HIT_EXTRA,
        trackW + KNOB_WIDTH,
        KNOB_HEIGHT + HIT_EXTRA * 2f);
    fillTrack.setBounds(trackX, trackY - TRACK_HEIGHT / 2f, trackW, TRACK_HEIGHT);
    syncLabelWidget();
    valueLabelText.layout();
  }

  // -------------------------------------------------------------------------
  // UiWidget overrides
  // -------------------------------------------------------------------------

  @Override
  public boolean hit(float worldX, float worldY) {
    if (bounds.contains(worldX, worldY)) {
      applyPointerX(worldX);
      return true;
    }
    return false;
  }

  @Override
  public void dragged(float worldX, float worldY) {
    applyPointerX(worldX);
  }

  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    float ratio = (value - min) / (max - min);
    float knobCentreX = trackX + ratio * trackW;

    // Track background + fill
    fillTrack.setFillRatio(ratio);
    fillTrack.draw(batch);

    // Knob
    batch.setColor(hovered ? COLOR_KNOB_HOVER : COLOR_KNOB);
    batch.draw(
        pixel, knobCentreX - KNOB_WIDTH / 2f, trackY - KNOB_HEIGHT / 2f, KNOB_WIDTH, KNOB_HEIGHT);

    batch.setColor(Color.WHITE);

    // Value label below the track (TextLabel handles colour and restore)
    valueLabelText.draw(batch);
  }

  // -------------------------------------------------------------------------
  // Internal
  // -------------------------------------------------------------------------

  private void applyPointerX(float worldX) {
    float clamped = MathUtils.clamp(worldX, trackX, trackX + trackW);
    float newValue = min + (clamped - trackX) / trackW * (max - min);
    newValue = MathUtils.clamp(newValue, min, max);
    if (newValue != value) {
      value = newValue;
      syncLabelWidget();
      onChanged.emit(value);
    }
  }

  /** Syncs the TextLabel text and position to the current value. */
  private void syncLabelWidget() {
    float knobCentreX = trackX + (value - min) / (max - min) * trackW;
    float labelY = trackY - TRACK_HEIGHT / 2f - 4f;
    valueLabelText.setText(String.format("%.1f", value));
    valueLabelText.setPosition(knobCentreX, labelY);
  }
}
