package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.cryptroot.core.event.Signal;
import java.util.Objects;

/**
 * A toggle checkbox widget.
 *
 * <p>Displays a square box to the left of a text label. The box is filled and shows a check-mark
 * glyph (✓) when checked; it is empty when unchecked.
 *
 * <p>State changes fire {@link #onChanged}:
 *
 * <pre>{@code
 * Checkbox cb = new Checkbox(skin, pixel, "Enable sound", 80f, 400f, false);
 * cb.onChanged.connect(checked -> label.setText("Sound: " + checked));
 * uiLayer.add(cb, 0);
 * }</pre>
 *
 * <p>{@code RadioGroup} also uses this class internally to build mutually exclusive groups; prefer
 * {@link RadioGroup} when only one option should be active at a time.
 */
public final class Checkbox extends BoundedWidget {

  private static final float BOX_PADDING = 6f; // padding inside the box border
  private static final float LABEL_GAP = 12f; // gap between box right edge and label
  private static final String CHECK_GLYPH = "\u2713";

  private static final Color COLOR_BOX_NORMAL = new Color(0.6f, 0.6f, 0.6f, 1f);
  private static final Color COLOR_BOX_CHECKED = new Color(0.8f, 0.8f, 1.0f, 1f);
  private static final Color COLOR_BOX_HOVER = new Color(0.75f, 0.75f, 0.85f, 1f);
  private static final Color COLOR_LABEL = Color.WHITE;

  /** Fires with the new checked state whenever the checkbox is toggled. */
  public final Signal<Boolean> onChanged = new Signal<>();

  private final UiSkin skin;
  private final Texture pixel;

  /** Side length of the square box, derived from the font's cap height. */
  private float boxSize;

  private boolean checked;

  /** Scratch for measuring, so layout allocates nothing. */
  private final Vector2 scratch = new Vector2();

  /** Renders the label to the right of the box. */
  private final TextLabel labelText;

  /** Renders the check glyph centred inside the box when checked. */
  private final TextLabel checkText;

  /** Draws the box border. Bounds set in {@link #layout()}. */
  private final PixelBorder boxBorder;

  /**
   * Creates a checkbox sized to its own box and label. Position and final size come from the
   * enclosing layout container, or from an explicit {@link #setBounds} call.
   *
   * @param skin provides the font (used for label and check glyph)
   * @param pixel 1×1 white texture for solid rect drawing
   * @param label text shown to the right of the box
   * @param initial initial checked state
   */
  public Checkbox(UiSkin skin, Texture pixel, String label, boolean initial) {
    Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(pixel, "pixel must not be null");
    Objects.requireNonNull(label, "label must not be null");
    this.skin = skin;
    this.pixel = pixel;
    this.checked = initial;

    labelText = new TextLabel(skin.font(), label, COLOR_LABEL);
    checkText = new TextLabel(skin.font(), CHECK_GLYPH, Color.BLACK);
    boxBorder = new PixelBorder(pixel, 2f, new Color(1f, 1f, 1f, 0.8f));

    // Registered as ordinary children so the CompositeWidget cascade lays them out and draws them.
    // The check glyph is toggled with setVisible rather than being drawn by hand.
    addChild(boxBorder);
    addChild(checkText);
    addChild(labelText);
  }

  /**
   * Creates a checkbox whose box left edge is at {@code x} with its label baseline at {@code y}.
   *
   * <p>Retained for hand-positioned screens; prefer {@link #Checkbox(UiSkin, Texture, String,
   * boolean)} inside a layout container.
   */
  public Checkbox(UiSkin skin, Texture pixel, String label, float x, float y, boolean initial) {
    this(skin, pixel, label, initial);
    preferredSize(scratch);
    setBounds(x, y - scratch.y, scratch.x, scratch.y);
  }

  public boolean isChecked() {
    return checked;
  }

  /**
   * Sets the checked state without emitting {@link #onChanged}. Used by {@link RadioGroup} when
   * enforcing mutual exclusion.
   */
  public void setCheckedSilent(boolean value) {
    checked = value;
  }

  // -------------------------------------------------------------------------
  // LayoutElement
  // -------------------------------------------------------------------------

  /** Natural size: the square box, a gap, and the measured label. */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    float box = UiHelper.barHeight(skin.font(), BOX_PADDING);
    return out.set(box + LABEL_GAP + labelText.getMeasuredWidth(), box);
  }

  // -------------------------------------------------------------------------
  // UiWidget implementation
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    // Fall back per axis, never both. A container often assigns one axis and leaves the other
    // zero for the widget to measure; replacing both would discard the size just assigned.
    if (frame.width <= 0f || frame.height <= 0f) {
      Vector2 natural = preferredSize(scratch);
      if (frame.width <= 0f) frame.width = natural.x;
      if (frame.height <= 0f) frame.height = natural.y;
    }
    bounds.set(frame);

    // The box is square and vertically centred in the frame, never taller than it.
    boxSize = Math.min(frame.height, UiHelper.barHeight(skin.font(), BOX_PADDING));
    float boxY = frame.y + (frame.height - boxSize) / 2f;

    boxBorder.setBounds(frame.x, boxY, boxSize, boxSize);

    checkText.setVisible(checked);
    checkText.setBoxAlign(Align.center);
    checkText.setBounds(frame.x, boxY, boxSize, boxSize);

    float labelX = frame.x + boxSize + LABEL_GAP;
    labelText.setBoxAlign(Align.left | Align.center);
    labelText.setBounds(
        labelX, frame.y, Math.max(0f, frame.x + frame.width - labelX), frame.height);
  }

  @Override
  public boolean hit(float worldX, float worldY) {
    if (bounds.contains(worldX, worldY)) {
      checked = !checked;
      onChanged.emit(checked);
      return true;
    }
    return false;
  }

  @Override
  public boolean update(float delta) {
    return false;
  }

  /**
   * Draws the box fill. The border, check glyph and label are registered children and are drawn
   * after this by the {@link CompositeWidget} cascade, so they land on top.
   */
  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    Color fill = checked ? COLOR_BOX_CHECKED : (hovered ? COLOR_BOX_HOVER : COLOR_BOX_NORMAL);
    batch.setColor(fill);
    batch.draw(pixel, boxBorder.getX(), boxBorder.getY(), boxSize, boxSize);
    batch.setColor(Color.WHITE);
  }
}
