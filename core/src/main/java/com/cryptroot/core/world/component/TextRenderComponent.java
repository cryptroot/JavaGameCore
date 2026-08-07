package com.cryptroot.core.world.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.ui.TextLabel;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.RenderComponent;
import java.util.Objects;

/**
 * Draws a line of text in world coordinates, aligned inside a world-space box — captions under
 * props, name plates, floor numbers, damage numbers.
 *
 * <p>Because it is an ordinary {@link RenderComponent}, the text participates in the normal render
 * passes: put it in {@link RenderPass#FOREGROUND_WORLD} to sit above every sprite, or in {@link
 * RenderPass#WORLD} to be Y-sorted along with them. It also means text scales with the scene when
 * viewed through a zoomed {@link com.cryptroot.core.render.SceneTransform} — which is what a
 * caption painted onto the world should do, and precisely why a tooltip (which should stay legible
 * at any zoom) is <em>not</em> this and is drawn in UI space instead.
 *
 * <p>Glyph layout is delegated to {@link TextLabel}, so the same dirty-flag caching that keeps UI
 * text from re-baking a {@code GlyphLayout} every frame applies here too.
 */
public final class TextRenderComponent implements RenderComponent, PositionComponent {

  private final TextLabel label;
  private final Rectangle box = new Rectangle();
  private final RenderPass renderPass;
  private final Color color = new Color(Color.WHITE);
  private final Color shadowColor = new Color();

  private boolean shadowed;
  private float shadowOffsetX = 1f;
  private float shadowOffsetY = -1f;
  private boolean visible = true;

  /**
   * @param font the face to draw with
   * @param text the initial text
   * @param x bottom-left world X of the box the text aligns within
   * @param y bottom-left world Y of the box
   * @param width box width in world units
   * @param height box height in world units
   * @param renderPass which render pass this text belongs to
   */
  public TextRenderComponent(
      BitmapFont font,
      String text,
      float x,
      float y,
      float width,
      float height,
      RenderPass renderPass) {
    Objects.requireNonNull(font, "font must not be null");
    Objects.requireNonNull(text, "text must not be null");
    this.renderPass = Objects.requireNonNull(renderPass, "renderPass must not be null");
    this.label = new TextLabel(font, text);
    this.label.setBoxAlign(Align.center);
    this.box.set(x, y, width, height);
  }

  // -------------------------------------------------------------------------
  // Configuration
  // -------------------------------------------------------------------------

  /** Replaces the drawn text. */
  public TextRenderComponent setText(String text) {
    label.setText(text);
    return this;
  }

  /** The current text. */
  public String text() {
    return label.getText();
  }

  /** Sets the draw colour (copied). */
  public TextRenderComponent setColor(Color newColor) {
    Objects.requireNonNull(newColor, "newColor must not be null");
    color.set(newColor);
    return this;
  }

  /**
   * Sets how the text aligns inside its box.
   *
   * @param gdxAlign a libGDX {@link Align} bitmask, e.g. {@code Align.bottom | Align.center}
   */
  public TextRenderComponent setAlign(int gdxAlign) {
    label.setBoxAlign(gdxAlign);
    return this;
  }

  /**
   * Draws a one-pass drop shadow behind the text, which is what keeps a caption readable over
   * arbitrary sprite colours underneath it.
   *
   * @param shadow the shadow colour (copied)
   * @param offsetX shadow X offset in world units
   * @param offsetY shadow Y offset in world units
   */
  public TextRenderComponent setShadow(Color shadow, float offsetX, float offsetY) {
    Objects.requireNonNull(shadow, "shadow must not be null");
    shadowColor.set(shadow);
    shadowOffsetX = offsetX;
    shadowOffsetY = offsetY;
    shadowed = true;
    return this;
  }

  /** Removes the drop shadow. */
  public TextRenderComponent clearShadow() {
    shadowed = false;
    return this;
  }

  /** Resizes the box the text aligns within, keeping its bottom-left corner. */
  public TextRenderComponent setSize(float width, float height) {
    box.setSize(width, height);
    return this;
  }

  /** When {@code false}, {@link #draw} is a no-op. Defaults to {@code true}. */
  public TextRenderComponent setVisible(boolean newVisible) {
    this.visible = newVisible;
    return this;
  }

  public boolean isVisible() {
    return visible;
  }

  // -------------------------------------------------------------------------
  // RenderComponent
  // -------------------------------------------------------------------------

  @Override
  public void draw(PolygonSpriteBatch batch) {
    if (!visible) return;
    label.setBounds(box.x, box.y, box.width, box.height);
    if (shadowed) {
      label.drawWithColorOffset(batch, shadowColor, shadowOffsetX, shadowOffsetY);
    }
    label.drawWithColor(batch, color);
  }

  @Override
  public RenderPass renderPass() {
    return renderPass;
  }

  @Override
  public float sortKey() {
    return box.y;
  }

  // -------------------------------------------------------------------------
  // PositionComponent
  // -------------------------------------------------------------------------

  @Override
  public float x() {
    return box.x;
  }

  @Override
  public float y() {
    return box.y;
  }

  @Override
  public void moveTo(float x, float y) {
    box.setPosition(x, y);
  }
}
