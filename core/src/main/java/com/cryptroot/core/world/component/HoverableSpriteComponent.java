package com.cryptroot.core.world.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.event.Signal0;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.BoundsComponent;
import com.cryptroot.core.world.ClickableComponent;
import com.cryptroot.core.world.RenderComponent;
import java.util.Objects;

/**
 * A clickable {@link TextureRegion} that darkens while the cursor is over it.
 *
 * <p>Bundles the three primitives needed for an interactive sprite into one component: {@link
 * RenderComponent} (draws the region, tinted on hover), {@link BoundsComponent} (axis-aligned hit
 * rectangle), and {@link ClickableComponent} (hover/click signals). Register a single instance:
 *
 * <pre>{@code
 * world.add(new WorldEntity().with(HoverableSpriteComponent.class, sprite));
 * }</pre>
 *
 * Auto-registration exposes it under {@code RenderComponent}, {@code BoundsComponent} and {@code
 * ClickableComponent}, so the core {@code HoverSystem} hit-tests it and fires the hover signals,
 * and the {@code ClickSystem} fires {@link #onClicked()}.
 *
 * <p>The only thing that varies between use sites is the target {@link RenderPass} — a UI hotspot,
 * a world prop, or a background element all share this same implementation. Pass the desired pass
 * at construction. In the {@code WORLD} pass {@link #sortKey()} returns the entity's Y so it
 * participates in painter's-algorithm Y-sorting; the other passes draw in insertion order and
 * ignore the sort key.
 *
 * <h3>Outline</h3>
 *
 * The white hover outline is <em>not</em> drawn here. Hover detection makes this entity the {@code
 * HoverSystem}'s hovered entity, and the {@link com.cryptroot.core.render.RenderPipeline} captures
 * + rings it through the {@link com.cryptroot.core.render.SelectionOutlineRenderer}. Because the
 * outline pass re-draws this component, the darkened appearance is preserved inside the ring.
 */
public final class HoverableSpriteComponent
    implements RenderComponent, BoundsComponent, ClickableComponent {

  /** Colour applied to the sprite when the cursor is over it (dark tint). */
  public static final Color DEFAULT_HOVER_TINT = new Color(0.45f, 0.45f, 0.45f, 1f);

  private final TextureRegion region;
  private final float x;
  private final float y;
  private final float w;
  private final float h;
  private final RenderPass renderPass;
  private final Color hoverTint;

  private final Signal0 onClicked = new Signal0();
  private final Signal0 onHoverEnter = new Signal0();
  private final Signal0 onHoverExit = new Signal0();

  private boolean hovered;

  /**
   * @param region the sprite frame, drawn at its native pixel size
   * @param x bottom-left position in world (scene) space
   * @param y bottom-left position in world (scene) space
   * @param renderPass the pass this sprite draws in (UI, WORLD, BACKGROUND, …)
   * @param hoverTint the colour applied to the sprite when the cursor is over it (dark tint)
   */
  public HoverableSpriteComponent(
      TextureRegion region, float x, float y, RenderPass renderPass, Color hoverTint) {
    Objects.requireNonNull(region, "region must not be null");
    Objects.requireNonNull(renderPass, "renderPass must not be null");
    Objects.requireNonNull(hoverTint, "hoverTint must not be null");
    this.region = region;
    this.x = x;
    this.y = y;
    this.w = region.getRegionWidth();
    this.h = region.getRegionHeight();
    this.renderPass = renderPass;
    this.hoverTint = hoverTint;

    onHoverEnter.connect(() -> hovered = true);
    onHoverExit.connect(() -> hovered = false);
  }

  /**
   * @param region the sprite frame, drawn at its native pixel size
   * @param x bottom-left position in world (scene) space
   * @param y bottom-left position in world (scene) space
   * @param renderPass the pass this sprite draws in (UI, WORLD, BACKGROUND, …)
   */
  public HoverableSpriteComponent(TextureRegion region, float x, float y, RenderPass renderPass) {
    this(region, x, y, renderPass, DEFAULT_HOVER_TINT);
  }

  /**
   * @param region the sprite frame
   * @param x bottom-left position in world (scene) space
   * @param y bottom-left position in world (scene) space
   * @param w draw width in world units, independent of the region's native pixel size
   * @param h draw height in world units, independent of the region's native pixel size
   * @param renderPass the pass this sprite draws in (UI, WORLD, BACKGROUND, …)
   * @param hoverTint the colour applied to the sprite when the cursor is over it (dark tint)
   */
  public HoverableSpriteComponent(
      TextureRegion region,
      float x,
      float y,
      float w,
      float h,
      RenderPass renderPass,
      Color hoverTint) {
    Objects.requireNonNull(region, "region must not be null");
    Objects.requireNonNull(renderPass, "renderPass must not be null");
    Objects.requireNonNull(hoverTint, "hoverTint must not be null");
    if (w <= 0f) {
      throw new IllegalArgumentException("w must be positive: " + w);
    }
    if (h <= 0f) {
      throw new IllegalArgumentException("h must be positive: " + h);
    }
    this.region = region;
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.renderPass = renderPass;
    this.hoverTint = hoverTint;

    onHoverEnter.connect(() -> hovered = true);
    onHoverExit.connect(() -> hovered = false);
  }

  /**
   * @param region the sprite frame
   * @param x bottom-left position in world (scene) space
   * @param y bottom-left position in world (scene) space
   * @param w draw width in world units, independent of the region's native pixel size
   * @param h draw height in world units, independent of the region's native pixel size
   * @param renderPass the pass this sprite draws in (UI, WORLD, BACKGROUND, …)
   */
  public HoverableSpriteComponent(
      TextureRegion region, float x, float y, float w, float h, RenderPass renderPass) {
    this(region, x, y, w, h, renderPass, DEFAULT_HOVER_TINT);
  }

  // -------------------------------------------------------------------------
  // RenderComponent
  // -------------------------------------------------------------------------

  @Override
  public void draw(PolygonSpriteBatch batch) {
    if (hovered) {
      batch.setColor(this.hoverTint);
      batch.draw(region, x, y, w, h);
      batch.setColor(Color.WHITE);
    } else {
      batch.draw(region, x, y, w, h);
    }
  }

  @Override
  public RenderPass renderPass() {
    return renderPass;
  }

  @Override
  public float sortKey() {
    return y;
  }

  // -------------------------------------------------------------------------
  // BoundsComponent
  // -------------------------------------------------------------------------

  @Override
  public boolean containsPoint(float wx, float wy) {
    return wx >= x && wx <= x + w && wy >= y && wy <= y + h;
  }

  @Override
  public Rectangle bounds(Rectangle out) {
    return out.set(x, y, w, h);
  }

  // -------------------------------------------------------------------------
  // ClickableComponent
  // -------------------------------------------------------------------------

  @Override
  public Signal0 onClicked() {
    return onClicked;
  }

  @Override
  public Signal0 onHoverEnter() {
    return onHoverEnter;
  }

  @Override
  public Signal0 onHoverExit() {
    return onHoverExit;
  }

  /** {@code true} when the cursor is currently over this sprite's bounds. */
  public boolean isHovered() {
    return hovered;
  }

  /**
   * The colour applied to the sprite when the cursor is over it (dark tint). {@code
   * DEFAULT_HOVER_TINT} is used if no custom tint is provided.
   */
  public Color hoverTint() {
    return hoverTint;
  }

  // -------------------------------------------------------------------------
  // Static factory methods
  // -------------------------------------------------------------------------

  public static HoverableSpriteComponent ui(
      TextureRegion region, float x, float y, Color hoverTint) {
    return new HoverableSpriteComponent(region, x, y, RenderPass.UI, hoverTint);
  }

  public static HoverableSpriteComponent defaultUi(TextureRegion region, float x, float y) {
    return HoverableSpriteComponent.ui(region, x, y, DEFAULT_HOVER_TINT);
  }
}
