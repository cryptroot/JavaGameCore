package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.event.Signal;
import java.util.Objects;

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

  /** Natural track width when nothing constrains the slider. */
  private static final float DEFAULT_TRACK_W = 200f;

  private final Texture pixel;
  private final TextLabel valueLabelText;
  private final FillTrack fillTrack;

  private final float min;
  private final float max;

  private float value;

  // Track geometry is derived from `frame` rather than stored, so the slider can be moved and
  // resized
  // by a layout container like any other widget. `frame` is the full interactive rectangle: the
  // track
  // plus the knob overhang on each side and the vertical hit slop above and below.

  /** Left edge of the track. */
  private float trackX() {
    return frame.x + KNOB_WIDTH / 2f;
  }

  /** Width of the track itself, excluding the knob overhang. */
  private float trackW() {
    return Math.max(0f, frame.width - KNOB_WIDTH);
  }

  /** Y centre of the track. */
  private float trackY() {
    return frame.y + HIT_EXTRA + KNOB_HEIGHT / 2f;
  }

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
    Objects.requireNonNull(pixel, "pixel must not be null");
    Objects.requireNonNull(font, "font must not be null");
    if (max <= min) {
      throw new IllegalArgumentException(
          "max must be greater than min: min=" + min + ", max=" + max);
    }
    this.pixel = pixel;
    this.min = min;
    this.max = max;
    // Label is centred around the knob x (targetWidth=0 = "centre around x" mode).
    valueLabelText = new TextLabel(font, "", 0f, 0f).setAlign(TextLabel.HAlign.CENTER, 0f);
    fillTrack = new FillTrack(pixel, COLOR_TRACK_BG, COLOR_TRACK_FILL);
    // Convert the track-centred coordinates into this widget's outer rectangle.
    setBounds(
        trackX - KNOB_WIDTH / 2f,
        trackY - KNOB_HEIGHT / 2f - HIT_EXTRA,
        trackW + KNOB_WIDTH,
        KNOB_HEIGHT + HIT_EXTRA * 2f);
    setValue(MathUtils.clamp(initial, min, max));
  }

  /**
   * Creates a slider sized by its enclosing layout container.
   *
   * @param min minimum value (inclusive)
   * @param max maximum value (inclusive); must be &gt; {@code min}
   * @param initial starting value; clamped to [{@code min}, {@code max}]
   */
  public Slider(Texture pixel, BitmapFont font, float min, float max, float initial) {
    this(pixel, font, 0f, 0f, DEFAULT_TRACK_W, min, max, initial);
    setBounds(0f, 0f, 0f, 0f);
  }

  /** Natural size: a default-width track plus the knob overhang and vertical hit slop. */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    return out.set(DEFAULT_TRACK_W + KNOB_WIDTH, KNOB_HEIGHT + HIT_EXTRA * 2f);
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
    if (frame.width <= 0f || frame.height <= 0f) {
      Vector2 natural = preferredSize(new Vector2());
      frame.setSize(natural.x, natural.y);
    }
    bounds.set(frame);
    fillTrack.setBounds(trackX(), trackY() - TRACK_HEIGHT / 2f, trackW(), TRACK_HEIGHT);
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
    float knobCentreX = trackX() + ratio * trackW();

    // Track background + fill
    fillTrack.setFillRatio(ratio);
    fillTrack.draw(batch);

    // Knob
    batch.setColor(hovered ? COLOR_KNOB_HOVER : COLOR_KNOB);
    batch.draw(
        pixel, knobCentreX - KNOB_WIDTH / 2f, trackY() - KNOB_HEIGHT / 2f, KNOB_WIDTH, KNOB_HEIGHT);

    batch.setColor(Color.WHITE);

    // Value label below the track (TextLabel handles colour and restore)
    valueLabelText.draw(batch);
  }

  // -------------------------------------------------------------------------
  // Internal
  // -------------------------------------------------------------------------

  private void applyPointerX(float worldX) {
    float tx = trackX();
    float tw = trackW();
    if (tw <= 0f) return;
    float clamped = MathUtils.clamp(worldX, tx, tx + tw);
    float newValue = min + (clamped - tx) / tw * (max - min);
    newValue = MathUtils.clamp(newValue, min, max);
    if (newValue != value) {
      value = newValue;
      syncLabelWidget();
      onChanged.emit(value);
    }
  }

  /** Syncs the TextLabel text and position to the current value. */
  private void syncLabelWidget() {
    float knobCentreX = trackX() + (value - min) / (max - min) * trackW();
    float labelY = trackY() - TRACK_HEIGHT / 2f - 4f;
    valueLabelText.setText(String.format("%.1f", value));
    valueLabelText.setPosition(knobCentreX, labelY);
  }
}
