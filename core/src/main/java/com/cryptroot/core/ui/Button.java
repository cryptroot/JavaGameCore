package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.event.Signal0;
import java.util.Objects;

/**
 * A bordered text button that implements the full {@link UiWidget} contract.
 *
 * <p>Navigation / action is driven by the public {@link #onClick} signal rather than a
 * constructor-time {@link Runnable}, so callers connect behaviour after construction and can
 * connect multiple listeners or disconnect them at any time:
 *
 * <pre>{@code
 * Button btn = new Button(skin, "Start Game", 200f, 500f);
 * btn.onClick.connect(game::showGameScreen);
 * uiLayer.add(btn, 0);
 * }</pre>
 *
 * <h3>Centred convenience</h3>
 *
 * Use {@link #centered(UiSkin, String, float)} when the button should be horizontally centred in
 * the 1600-unit world:
 *
 * <pre>{@code
 * Button btn = Button.centered(menuSkin, "1   MASCOTS", 590f);
 * btn.onClick.connect(game::showMascotScreen);
 * }</pre>
 *
 * <h3>Click feedback</h3>
 *
 * When the button is clicked (or {@link #triggerClick()} is called directly for keyboard
 * shortcuts), the selected nine-patch slice renders for a short delay before {@link #onClick}
 * fires. This behaviour matches the original {@code MenuButton} exactly.
 *
 * <h3>Frame-consumption contract</h3>
 *
 * {@link #update(float)} returns {@code true} on the frame that the click-delay expires and {@link
 * #onClick} fires. The owning screen must return from {@code render()} immediately at that point.
 *
 * <h3>Subclassing</h3>
 *
 * {@code Button} is open for subclassing. Subclasses that need different click behaviour (e.g.,
 * instant activation without the feedback delay) may override {@link #hit(float, float)} and {@link
 * #update(float)}, and have access to the protected fields {@link #skin}, {@link #clicked}, and
 * {@link #clickTimer}.
 */
public class Button extends BoundedWidget {

  // World-unit constants — the world is always 1600 wide regardless of window size.
  private static final float WORLD_WIDTH = 1600f;

  /** Seconds the selected slice is shown before onClick fires. */
  private static final float CLICK_FEEDBACK_DELAY = 0.08f;

  /** RGB multiplier applied to the normal slice on hover (~35% darkening). */
  private static final float HOVER_DARKEN = 0.65f;

  // Padding around the text glyph bounds that determines nine-patch size.
  private static final float PAD_H = 20f;
  private static final float PAD_V_BOT = 10f;
  private static final float PAD_V_TOP = 20f;

  /** Fires when the button is clicked (after the visual feedback delay). */
  public final Signal0 onClick = new Signal0();

  protected final UiSkin skin;
  private final String label;
  private float textX;
  private float textY;
  private final boolean centred;
  private final TextLabel labelWidget;

  protected boolean clicked;
  protected float clickTimer;

  /**
   * Creates a button whose text baseline is at {@code (textX, textY)} in world coordinates. Use
   * {@link #centered(UiSkin, String, float)} when horizontal centering across the full world width
   * is desired.
   */
  public Button(UiSkin skin, String label, float textX, float textY) {
    Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(label, "label must not be null");
    this.skin = skin;
    this.label = label;
    this.textX = textX;
    this.textY = textY;
    this.centred = false;
    labelWidget = new TextLabel(skin.font(), label, textX, textY).setColor(Color.BLACK.cpy());
    addChild(labelWidget);
  }

  /**
   * Repositions a non-centred button. For centred buttons this is a no-op. A subsequent {@link
   * #layout()} call (triggered by the parent composite) will remeasure and reposition the button at
   * the new coordinates.
   */
  @Override
  public void setPosition(float x, float y) {
    if (!centred) {
      textX = x;
      textY = y;
    }
  }

  /**
   * Sets the label text colour. Returns {@code this} for fluent chaining.
   *
   * <pre>{@code
   * Button btn = new Button(skin, "Start", 200f, 500f)
   *         .setLabelColour(Color.BLACK);
   * }</pre>
   */
  public Button setLabelColour(Color colour) {
    Objects.requireNonNull(colour, "colour must not be null");
    labelWidget.setColor(colour);
    return this;
  }

  private Button(UiSkin skin, String label, float textY, boolean centred) {
    this.skin = skin;
    this.label = label;
    this.textX = 0f;
    this.textY = textY;
    this.centred = true;
    labelWidget =
        new TextLabel(skin.font(), label, 0f, textY)
            .setAlign(TextLabel.HAlign.CENTER, WORLD_WIDTH)
            .setColor(Color.BLACK.cpy());
    addChild(labelWidget);
  }

  /**
   * Creates a button whose label is horizontally centred in the 1600-unit world at the given text
   * baseline Y.
   */
  public static Button centered(UiSkin skin, String label, float textY) {
    Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(label, "label must not be null");
    return new Button(skin, label, textY, true);
  }

  // -------------------------------------------------------------------------
  // CompositeWidget
  // -------------------------------------------------------------------------

  /**
   * Positions the label (computing centred X when applicable) and derives the nine-patch background
   * bounds from the measured text size. TextLabel's internal {@link
   * com.badlogic.gdx.graphics.g2d.GlyphLayout} is reused for the bounds calculation — no separate
   * allocation needed.
   */
  @Override
  protected void doBoundedLayout() {
    if (centred) {
      labelWidget.setAlign(TextLabel.HAlign.CENTER, WORLD_WIDTH);
      labelWidget.setPosition(0f, textY);
    } else {
      labelWidget.setPosition(textX, textY);
    }
    labelWidget.layout(); // remeasure so getMeasured* are available below
    float glW = labelWidget.getMeasuredWidth();
    float glH = labelWidget.getMeasuredHeight();
    float lx = labelWidget.getDrawX();
    bounds.set(lx - PAD_H, textY - glH - PAD_V_BOT, glW + PAD_H * 2f, glH + PAD_V_BOT + PAD_V_TOP);
  }

  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    if (clicked) {
      skin.selectedSlice().draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);
    } else if (hovered) {
      batch.setColor(HOVER_DARKEN, HOVER_DARKEN, HOVER_DARKEN, 1f);
      skin.normalSlice().draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);
      batch.setColor(Color.WHITE);
    } else {
      skin.normalSlice().draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);
    }
  }

  @Override
  public void updateHover(float worldX, float worldY) {
    hovered = !clicked && bounds.contains(worldX, worldY);
  }

  /** Intercepts hit at the composite bounds level; does not delegate to children. */
  @Override
  public boolean hit(float worldX, float worldY) {
    if (bounds.contains(worldX, worldY)) {
      triggerClick();
      return true;
    }
    return false;
  }

  @Override
  public boolean update(float delta) {
    if (clicked) {
      clickTimer -= delta;
      if (clickTimer <= 0f) {
        clicked = false;
        onClick.emit();
        return true;
      }
    }
    return super.update(delta);
  }

  @Override
  protected void doBoundedReset() {
    clicked = false;
    clickTimer = 0f;
  }

  // -------------------------------------------------------------------------
  // Keyboard shortcut support
  // -------------------------------------------------------------------------

  /**
   * Arms click feedback directly without a hit test. Use for keyboard shortcuts so the
   * selected-slice visual fires even when the cursor is not over the button.
   */
  public void triggerClick() {
    clicked = true;
    hovered = false;
    clickTimer = CLICK_FEEDBACK_DELAY;
  }
}
