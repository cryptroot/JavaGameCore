package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.cryptroot.core.event.Signal0;
import com.cryptroot.core.ui.layout.Insets;
import java.util.Objects;

/**
 * A bordered text button that implements the full {@link UiWidget} contract.
 *
 * <p>Navigation / action is driven by the public {@link #onClick} signal rather than a
 * constructor-time {@link Runnable}, so callers connect behaviour after construction and can
 * connect multiple listeners or disconnect them at any time:
 *
 * <pre>{@code
 * Button btn = new Button(skin, "Start Game");
 * btn.onClick.connect(game::showGameScreen);
 * layoutContainer.add(btn);
 * }</pre>
 *
 * <h3>Sizing</h3>
 *
 * The button's natural size is its measured label plus the skin theme's {@linkplain
 * UiTheme#controlPadding() control padding}, reported through {@link #preferredSize(Vector2)}. A
 * layout container reads that, assigns a final rectangle with {@link #setBounds}, and the label is
 * aligned inside it — so row spacing can never disagree with button height.
 *
 * <p>A button added straight to a {@link UiLayer} with no container and no explicit bounds falls
 * back to its natural size, so it still renders rather than collapsing to nothing. The fallback is
 * per axis: assign only a width and the height is still measured from the label, and vice versa.
 *
 * <p>There is deliberately no "centre in the world" helper. Centring is expressed as {@link
 * #align(int) align(Align.center)} within whatever rectangle the parent assigns, which works at any
 * resolution; the previous {@code centered(skin, label, y)} factory hard-coded a 1600-unit world
 * width and silently mispositioned every button in a world of any other size.
 *
 * <h3>Click feedback</h3>
 *
 * When the button is clicked (or {@link #triggerClick()} is called directly for keyboard
 * shortcuts), the selected nine-patch slice renders for a short delay before {@link #onClick}
 * fires.
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

  /** Seconds the selected slice is shown before onClick fires. */
  private static final float CLICK_FEEDBACK_DELAY = 0.08f;

  /** RGB multiplier applied to the normal slice on hover (~35% darkening). */
  private static final float HOVER_DARKEN = 0.65f;

  /** Fires when the button is clicked (after the visual feedback delay). */
  public final Signal0 onClick = new Signal0();

  protected final UiSkin skin;
  private final TextLabel labelWidget;
  private int labelAlign = Align.center;

  /** Scratch for measuring, so layout allocates nothing. */
  private final Vector2 scratch = new Vector2();

  protected boolean clicked;
  protected float clickTimer;

  /**
   * Creates a button sized to its own label. Position and final size come from the enclosing layout
   * container, or from an explicit {@link #setBounds} call.
   */
  public Button(UiSkin skin, String label) {
    Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(label, "label must not be null");
    this.skin = skin;
    this.labelWidget = new TextLabel(skin.font(), label).setColor(Color.BLACK.cpy());
    addChild(labelWidget);
  }

  /**
   * Creates a button whose <em>text baseline</em> sits at {@code (textX, textY)}, deriving the
   * border rectangle around it from the measured label and the theme's control padding.
   *
   * <p>Retained for screens that still position widgets by hand. Prefer {@link #Button(UiSkin,
   * String)} plus a layout container: manual coordinates are what caused rows of buttons to
   * overlap, because a caller choosing a row pitch has no way to know the height this constructor
   * will produce.
   */
  public Button(UiSkin skin, String label, float textX, float textY) {
    this(skin, label);
    Insets pad = skin.theme().controlPadding();
    preferredSize(scratch);
    setBounds(
        textX - pad.left(),
        textY - skin.font().getCapHeight() - pad.bottom(),
        scratch.x,
        scratch.y);
  }

  /**
   * Sets how the label is aligned inside the button's rectangle. Defaults to {@link Align#center}.
   *
   * @param gdxAlign a libGDX {@link Align} bitmask
   */
  public Button align(int gdxAlign) {
    this.labelAlign = gdxAlign;
    return this;
  }

  /**
   * Sets the label text colour. Returns {@code this} for fluent chaining.
   *
   * <pre>{@code
   * Button btn = new Button(skin, "Start").setLabelColour(Color.BLACK);
   * }</pre>
   */
  public Button setLabelColour(Color colour) {
    Objects.requireNonNull(colour, "colour must not be null");
    labelWidget.setColor(colour);
    return this;
  }

  /** Replaces the label text; the button's natural size changes accordingly. */
  public Button setLabel(String label) {
    labelWidget.setText(label);
    return this;
  }

  // -------------------------------------------------------------------------
  // LayoutElement
  // -------------------------------------------------------------------------

  /** Natural size: the measured label plus the theme's control padding. */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    return preferredSizeFor(
        labelWidget.getMeasuredWidth(),
        skin.font().getCapHeight(),
        skin.theme().controlPadding(),
        out);
  }

  /**
   * The size a button needs for a label of the given measured width, drawn in a font of the given
   * cap height, with the given padding.
   *
   * <p>Extracted as a pure static so the sizing rule is unit-testable without GL — this is the
   * arithmetic that determines whether a column of buttons fits its row pitch.
   *
   * @return {@code out}, set to {@code (textWidth + padding.horizontal(), capHeight +
   *     padding.vertical())}
   */
  public static Vector2 preferredSizeFor(
      float textWidth, float capHeight, Insets padding, Vector2 out) {
    Objects.requireNonNull(padding, "padding must not be null");
    Objects.requireNonNull(out, "out must not be null");
    return out.set(textWidth + padding.horizontal(), capHeight + padding.vertical());
  }

  // -------------------------------------------------------------------------
  // CompositeWidget
  // -------------------------------------------------------------------------

  /**
   * Adopts the natural size if no rectangle has been assigned yet, then aligns the label inside the
   * padded content area.
   */
  @Override
  protected void doBoundedLayout() {
    // Fall back per axis, never both. A container that stretches a row assigns the width and
    // leaves the height zero for the button to measure from its label, and vice versa; replacing
    // both would discard whichever axis the container had already decided.
    if (frame.width <= 0f || frame.height <= 0f) {
      Vector2 natural = preferredSize(scratch);
      if (frame.width <= 0f) frame.width = natural.x;
      if (frame.height <= 0f) frame.height = natural.y;
    }
    bounds.set(frame);

    Insets pad = skin.theme().controlPadding();
    labelWidget.setBoxAlign(labelAlign);
    labelWidget.setBounds(
        frame.x + pad.left(),
        frame.y + pad.bottom(),
        frame.width - pad.horizontal(),
        frame.height - pad.vertical());
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
