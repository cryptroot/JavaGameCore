package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.cryptroot.core.ui.layout.LayoutElement;
import java.util.Objects;

/**
 * A self-contained, display-only text widget.
 *
 * <p>{@code TextLabel} owns a {@link GlyphLayout} that is baked only when the text, position,
 * alignment or draw colour actually changes (dirty-flag pattern) and reused every frame,
 * eliminating the per-frame {@code new GlyphLayout()} allocations that would otherwise appear in
 * every composite widget's {@code draw()} method.
 *
 * <h3>Two positioning modes</h3>
 *
 * <ul>
 *   <li><b>Baseline mode</b> (default) — the constructor and {@link #setPosition} place the text
 *       baseline directly, with {@link #setAlign(HAlign, float)} controlling horizontal placement
 *       relative to that point. This is the original behaviour and is what widgets that
 *       hand-compute text positions rely on.
 *   <li><b>Box mode</b> — {@link #setBounds} assigns an outer rectangle and the label aligns itself
 *       inside it according to {@link #setBoxAlign(int)}. This is the {@link LayoutElement} path
 *       used by layout containers, and the reason a container never has to know anything about
 *       baselines.
 * </ul>
 *
 * <p>Calling {@link #setBounds} switches to box mode; calling {@link #setPosition} switches back.
 *
 * <h3>Baseline-mode alignment</h3>
 *
 * <ul>
 *   <li>{@link HAlign#LEFT} (default) — text starts at {@code x}.
 *   <li>{@link HAlign#CENTER} / {@link HAlign#RIGHT} with {@code targetWidth == 0} — text is
 *       centred around, or ends at, {@code x}.
 *   <li>{@link HAlign#CENTER} / {@link HAlign#RIGHT} with {@code targetWidth > 0} — text is aligned
 *       within the region {@code [x, x + targetWidth]}.
 * </ul>
 *
 * <h3>Usage — standalone</h3>
 *
 * <pre>{@code
 * TextLabel versionLabel = new TextLabel(context.assets().font(FontSize.HINT), "v0.1", 80f, 30f);
 * uiLayer.add(versionLabel, 0);
 * }</pre>
 *
 * <h3>Usage — inside a layout container</h3>
 *
 * <pre>{@code
 * // No coordinates at all: the VStack assigns bounds, the label aligns within them.
 * stack.add(new TextLabel(font, "Rooms", 0f, 0f).setBoxAlign(Align.left | Align.center));
 * }</pre>
 */
public final class TextLabel implements LayoutElement {

  /** Horizontal alignment mode for baseline-mode placement. */
  public enum HAlign {
    LEFT,
    CENTER,
    RIGHT
  }

  /** Box-mode default: text hugs the left edge, vertically centred. */
  private static final int DEFAULT_BOX_ALIGN = Align.left | Align.center;

  private final BitmapFont font;
  private String text;
  private Color color; // always a private copy — never the source constant
  private float x;
  private float y; // BitmapFont baseline Y (baseline mode)
  private HAlign align = HAlign.LEFT;
  private float targetWidth = 0f;

  /** Outer rectangle assigned by {@link #setBounds}; only meaningful while {@link #boxMode}. */
  private final Rectangle frame = new Rectangle();

  private boolean boxMode;
  private int boxAlign = DEFAULT_BOX_ALIGN;
  private boolean visible = true;

  // Cached layout — rebaked only when dirty, or when a different draw colour is requested.
  private final GlyphLayout glyphLayout = new GlyphLayout();
  private final Color bakedColor = new Color(Color.WHITE);
  private boolean dirty = true;
  private float drawX; // resolved draw X after alignment
  private float drawY; // resolved baseline Y

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public TextLabel(BitmapFont font, String text, float x, float y) {
    Objects.requireNonNull(font, "font must not be null");
    Objects.requireNonNull(text, "text must not be null");
    this.font = font;
    this.text = text;
    this.x = x;
    this.y = y;
    this.color = Color.WHITE.cpy();
  }

  public TextLabel(BitmapFont font, String text, float x, float y, Color color) {
    this(font, text, x, y);
    Objects.requireNonNull(color, "color must not be null");
    this.color = color.cpy();
  }

  /**
   * Creates a label with no initial position, for use inside a layout container that will assign
   * its bounds. Equivalent to {@code new TextLabel(font, text, 0f, 0f)}.
   */
  public TextLabel(BitmapFont font, String text) {
    this(font, text, 0f, 0f);
  }

  /** As {@link #TextLabel(BitmapFont, String)}, with an initial colour (copied). */
  public TextLabel(BitmapFont font, String text, Color color) {
    this(font, text, 0f, 0f, color);
  }

  // -------------------------------------------------------------------------
  // Fluent setters
  // -------------------------------------------------------------------------

  /** Updates the displayed text. Marks the layout dirty. Returns {@code this} for chaining. */
  public TextLabel setText(String newText) {
    Objects.requireNonNull(newText, "newText must not be null");
    if (!newText.equals(text)) {
      text = newText;
      dirty = true;
    }
    return this;
  }

  /** Returns the current text string. */
  public String getText() {
    return text;
  }

  /** Sets the draw colour (copied). Returns {@code this} for chaining. */
  public TextLabel setColor(Color newColor) {
    Objects.requireNonNull(newColor, "newColor must not be null");
    color = newColor.cpy();
    dirty = true;
    return this;
  }

  /**
   * Configures baseline-mode alignment. Returns {@code this} for chaining.
   *
   * @param align where the text sits relative to {@code x} / the target region
   * @param targetWidth width of the region to align within; pass {@code 0} to align relative to
   *     {@code x} itself
   */
  public TextLabel setAlign(HAlign align, float targetWidth) {
    Objects.requireNonNull(align, "align must not be null");
    this.align = align;
    this.targetWidth = targetWidth;
    dirty = true;
    return this;
  }

  /**
   * Configures how the text aligns inside the rectangle given to {@link #setBounds} (box mode).
   *
   * @param gdxAlign a libGDX {@link Align} bitmask, e.g. {@code Align.right | Align.top}; defaults
   *     to {@code Align.left | Align.center}
   */
  public TextLabel setBoxAlign(int gdxAlign) {
    this.boxAlign = gdxAlign;
    dirty = true;
    return this;
  }

  // -------------------------------------------------------------------------
  // Measured dimensions — available after layout() or draw()
  // -------------------------------------------------------------------------

  /** Width of the rendered text in world units. Forces a bake if dirty. */
  public float getMeasuredWidth() {
    ensureBaked();
    return glyphLayout.width;
  }

  /**
   * Height of the rendered text's glyph bounds in world units. Forces a bake if dirty.
   *
   * <p>Note this is the {@link GlyphLayout}'s height, which varies with the actual characters
   * present (ascenders, brackets, descenders). For laying out uniform rows prefer {@link
   * #preferredSize}, which reports the font's cap height so that rows do not jitter as their text
   * changes.
   */
  public float getMeasuredHeight() {
    ensureBaked();
    return glyphLayout.height;
  }

  /** Resolved draw X after alignment is applied. Forces a bake if dirty. */
  public float getDrawX() {
    ensureBaked();
    return drawX;
  }

  /** Resolved text baseline Y after alignment is applied. Forces a bake if dirty. */
  public float getDrawY() {
    ensureBaked();
    return drawY;
  }

  // -------------------------------------------------------------------------
  // LayoutElement
  // -------------------------------------------------------------------------

  /**
   * Natural size: the measured text width by the font's cap height.
   *
   * <p>Cap height rather than {@link #getMeasuredHeight()} deliberately — it is constant for a
   * given font, so a column of labels has uniform row heights regardless of which characters each
   * row happens to contain.
   */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    ensureBaked();
    return out.set(glyphLayout.width, font.getCapHeight());
  }

  /** Switches to box mode and aligns the text within {@code (x, y, width, height)}. */
  @Override
  public void setBounds(float x, float y, float width, float height) {
    frame.set(x, y, width, height);
    boxMode = true;
    dirty = true;
  }

  /**
   * Repositions the text baseline and marks the layout dirty, switching back to baseline mode if
   * {@link #setBounds} had been used.
   */
  @Override
  public void setPosition(float newX, float newY) {
    if (this.x != newX || this.y != newY || boxMode) {
      this.x = newX;
      this.y = newY;
      boxMode = false;
      dirty = true;
    }
  }

  // -------------------------------------------------------------------------
  // UiWidget — non-interactive
  // -------------------------------------------------------------------------

  /** Forces an immediate bake and caches the resolved draw position. */
  @Override
  public void layout() {
    bake(color);
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

  /**
   * Shows or hides the label. Hidden labels skip {@link #draw(PolygonSpriteBatch)} but keep
   * measuring and laying out, so a container's geometry does not jump when one is toggled.
   *
   * <p>Lets a composite register a conditionally drawn label as an ordinary child instead of
   * holding it in a side list and hand-drawing it — see {@link Checkbox}'s check glyph.
   */
  public TextLabel setVisible(boolean visible) {
    this.visible = visible;
    return this;
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  /** Draws the text in its stored colour, baking lazily if needed. Skipped when not visible. */
  @Override
  public void draw(PolygonSpriteBatch batch) {
    if (!visible) return;
    drawWithColor(batch, color);
  }

  /**
   * Draws the text using {@code colorOverride} instead of the stored colour, without mutating the
   * stored colour.
   *
   * <p>Intended for widgets (e.g. {@link ScrollList}) that vary per-item colour for hover /
   * selection without allocating a {@link Color} every frame.
   *
   * <p>The colour has to be baked into the {@link GlyphLayout}: {@code font.draw(batch, layout, …)}
   * reads per-glyph colours recorded by {@code GlyphLayout.setText}, so setting the font colour
   * afterwards would have no effect. The baked colour is therefore cached and the layout is rebaked
   * only when a genuinely different colour is asked for — so alternating hover states cost one bake
   * each, not one bake per frame.
   */
  public void drawWithColor(PolygonSpriteBatch batch, Color colorOverride) {
    Objects.requireNonNull(colorOverride, "colorOverride must not be null");
    if (dirty || !bakedColor.equals(colorOverride)) {
      bake(colorOverride);
    }
    font.draw(batch, glyphLayout, drawX, drawY);
  }

  /**
   * Like {@link #drawWithColor} but draws at {@code (drawX + dxOffset, drawY + dyOffset)}. Intended
   * for a shadow pass immediately followed by a call to {@link #drawWithColor}.
   */
  public void drawWithColorOffset(
      PolygonSpriteBatch batch, Color colorOverride, float dxOffset, float dyOffset) {
    Objects.requireNonNull(colorOverride, "colorOverride must not be null");
    if (dirty || !bakedColor.equals(colorOverride)) {
      bake(colorOverride);
    }
    font.draw(batch, glyphLayout, drawX + dxOffset, drawY + dyOffset);
  }

  // -------------------------------------------------------------------------
  // Private
  // -------------------------------------------------------------------------

  private void ensureBaked() {
    if (dirty) bake(color);
  }

  /**
   * Rebuilds the glyph layout with {@code bakeWith} as the glyph colour and resolves the draw
   * position from the current mode.
   */
  private void bake(Color bakeWith) {
    font.setColor(bakeWith);
    glyphLayout.setText(font, text);
    font.setColor(Color.WHITE);
    bakedColor.set(bakeWith);

    if (boxMode) {
      drawX = UiHelper.alignIn(frame.x, frame.width, glyphLayout.width, boxAlign);
      drawY = UiHelper.baselineIn(frame.y, frame.height, font.getCapHeight(), boxAlign);
    } else {
      drawX =
          switch (align) {
            case CENTER ->
                targetWidth > 0f
                    ? x + (targetWidth - glyphLayout.width) / 2f
                    : x - glyphLayout.width / 2f;
            case RIGHT ->
                targetWidth > 0f ? x + targetWidth - glyphLayout.width : x - glyphLayout.width;
            default -> x;
          };
      drawY = y;
    }
    dirty = false;
  }
}
