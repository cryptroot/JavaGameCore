package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.event.Signal;

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
  private final String label;
  private float x;
  private float y; // text baseline

  private float boxSize;
  private boolean checked;

  /** Renders the label to the right of the box. */
  private final TextLabel labelText;

  /** Renders the check glyph centred inside the box when checked. */
  private final TextLabel checkText;

  /** Draws the box border. Bounds set in {@link #layout()}. */
  private final PixelBorder boxBorder;

  /**
   * @param skin provides the font (used for label and check glyph)
   * @param pixel 1×1 white texture for solid rect drawing
   * @param label text shown to the right of the box
   * @param x left edge of the box in world coordinates
   * @param y text baseline in world coordinates
   * @param initial initial checked state
   */
  public Checkbox(UiSkin skin, Texture pixel, String label, float x, float y, boolean initial) {
    this.skin = skin;
    this.pixel = pixel;
    this.label = label;
    this.x = x;
    this.y = y;
    this.checked = initial;
    // Positions are placeholder until layout() is called.
    labelText = new TextLabel(skin.font(), label, 0f, 0f, COLOR_LABEL);
    checkText =
        new TextLabel(skin.font(), CHECK_GLYPH, 0f, 0f, Color.BLACK)
            .setAlign(TextLabel.HAlign.CENTER, 0f);
    boxBorder = new PixelBorder(pixel, 2f, new Color(1f, 1f, 1f, 0.8f));
  }

  public boolean isChecked() {
    return checked;
  }

  /**
   * Repositions the checkbox. Call {@link #layout()} afterwards to apply (a {@link CompositeWidget}
   * parent does this automatically).
   */
  @Override
  public void setPosition(float newX, float newY) {
    this.x = newX;
    this.y = newY;
  }

  /**
   * Sets the checked state without emitting {@link #onChanged}. Used by {@link RadioGroup} when
   * enforcing mutual exclusion.
   */
  public void setCheckedSilent(boolean value) {
    checked = value;
  }

  // -------------------------------------------------------------------------
  // UiWidget implementation
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    boxSize = UiHelper.barHeight(skin.font(), BOX_PADDING);
    float totalWidth = boxSize + LABEL_GAP + labelText.getMeasuredWidth();
    // Bounds cover box + label; y-origin = bottom of box.
    bounds.set(x, y - boxSize, totalWidth, boxSize);

    // Label: baseline at y, to the right of the box.
    labelText.setPosition(x + boxSize + LABEL_GAP, y);
    labelText.layout();

    // Check glyph: centred around box centre X; baseline computed from capHeight.
    float boxCentreX = x + boxSize / 2f;
    float checkY = y - (boxSize - skin.font().getCapHeight()) / 2f;
    checkText.setPosition(boxCentreX, checkY);
    checkText.layout();

    boxBorder.setBounds(x, y - boxSize, boxSize, boxSize);
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

  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    // Box fill
    Color fill = checked ? COLOR_BOX_CHECKED : (hovered ? COLOR_BOX_HOVER : COLOR_BOX_NORMAL);
    batch.setColor(fill);
    batch.draw(pixel, x, y - boxSize, boxSize, boxSize);

    boxBorder.draw(batch);

    // Check glyph (TextLabel handles colour = Color.BLACK and restore)
    if (checked) checkText.draw(batch);

    // Label (TextLabel handles colour = COLOR_LABEL and restore)
    labelText.draw(batch);
  }
}
