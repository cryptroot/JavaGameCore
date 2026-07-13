package com.cryptroot.core.world.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.RenderComponent;
import java.util.Objects;

/**
 * Renders a static {@link TextureRegion} at a configurable world position.
 *
 * <p>Implements both {@link RenderComponent} and {@link PositionComponent}. Moving the entity via
 * {@link #moveTo} repositions the drawn quad.
 *
 * <p>The {@link #sortKey()} returns the entity's world Y ({@link #y()}), so static props placed in
 * the {@link RenderLayer#WORLD} layer participate correctly in painter's-algorithm Y-sorting.
 *
 * <p>Two optional, independent knobs support common overlay use cases (a translucent placement
 * ghost, a hide-when-off-grid indicator, …) without a decorator: {@link #setTint} multiplies the
 * drawn colour (defaults to opaque white, i.e. the region's native colours), and {@link
 * #setVisible} skips drawing entirely while kept {@code false} (defaults to {@code true}).
 */
public final class TextureRenderComponent implements RenderComponent, PositionComponent {

  private final TextureRegion region;
  private float x;
  private float y;
  private final float width;
  private final float height;
  private final RenderPass renderPass;
  private final Color tint = new Color(Color.WHITE);
  private final Color batchColorScratch = new Color();
  private boolean visible = true;

  /**
   * @param region the texture to draw
   * @param x bottom-left world X
   * @param y bottom-left world Y
   * @param width draw width in world units
   * @param height draw height in world units
   * @param renderPass which render pass this entity belongs to
   */
  public TextureRenderComponent(
      TextureRegion region, float x, float y, float width, float height, RenderPass renderPass) {
    Objects.requireNonNull(region, "region must not be null");
    Objects.requireNonNull(renderPass, "renderPass must not be null");
    if (width <= 0f) {
      throw new IllegalArgumentException("width must be positive: " + width);
    }
    if (height <= 0f) {
      throw new IllegalArgumentException("height must be positive: " + height);
    }
    this.region = region;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.renderPass = renderPass;
  }

  /** Multiplies the drawn colour by {@code tint} (copied); {@link Color#WHITE} draws unmodified. */
  public void setTint(Color tint) {
    Objects.requireNonNull(tint, "tint must not be null");
    this.tint.set(tint);
  }

  /** The current tint (a defensive copy). */
  public Color tint() {
    return new Color(tint);
  }

  /** When {@code false}, {@link #draw} is a no-op. Defaults to {@code true}. */
  public void setVisible(boolean visible) {
    this.visible = visible;
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
    batchColorScratch.set(batch.getColor());
    batch.setColor(tint);
    batch.draw(region, x, y, width, height);
    batch.setColor(batchColorScratch);
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
  // PositionComponent
  // -------------------------------------------------------------------------

  @Override
  public float x() {
    return x;
  }

  @Override
  public float y() {
    return y;
  }

  @Override
  public void moveTo(float x, float y) {
    this.x = x;
    this.y = y;
  }
}
