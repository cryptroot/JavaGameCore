package com.cryptroot.core.world.component;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.render.NormalMappedDrawable;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.UpdateComponent;
import java.util.Objects;

/**
 * Renders a unit using the normal-mapped lighting path ({@link
 * com.cryptroot.core.render.NormalMappedRenderer}).
 *
 * <p>This component does <strong>not</strong> implement {@link
 * com.cryptroot.core.world.RenderComponent}. Instead, {@link
 * com.cryptroot.core.world.WorldEntityLayer#drawNormalMapped} detects entities that carry this
 * component and draws them in a separate pass that wraps the {@code NormalMappedSpineRenderer}
 * begin/end lifecycle correctly.
 *
 * <p>The component implements {@link UpdateComponent} (animation ticking) and {@link
 * PositionComponent} (position tracking), both of which are handled by the regular {@code update()}
 * pass and are not affected by the separate render pass.
 *
 * <p>Entities are Y-sorted within the normal-mapped pass using {@link #sortKey()}, consistent with
 * standard {@code WORLD}-layer entities. Note that interleaving between standard and NM entities is
 * not guaranteed — all NM entities are drawn after all standard WORLD entities.
 */
public final class NormalMappedRenderComponent
    implements RenderComponent, UpdateComponent, PositionComponent {

  private final NormalMappedDrawable instance;
  private float posX;
  private float posY;
  private final Rectangle boundsCache = new Rectangle();

  public NormalMappedRenderComponent(
      NormalMappedDrawable instance, float initialX, float initialY) {
    Objects.requireNonNull(instance, "instance must not be null");
    this.instance = instance;
    this.posX = initialX;
    this.posY = initialY;
  }

  /** Convenience constructor — position defaults to {@code (0, 0)}. */
  public NormalMappedRenderComponent(NormalMappedDrawable instance) {
    this(instance, 0f, 0f);
  }

  // -------------------------------------------------------------------------
  // UpdateComponent
  // -------------------------------------------------------------------------

  @Override
  public void update(float delta) {
    instance.update(delta);
  }

  // -------------------------------------------------------------------------
  // PositionComponent
  // -------------------------------------------------------------------------

  @Override
  public float x() {
    return posX;
  }

  @Override
  public float y() {
    return posY;
  }

  @Override
  public void moveTo(float x, float y) {
    posX = x;
    posY = y;
    instance.setPosition(x, y);
  }

  // -------------------------------------------------------------------------
  // Sort / layer metadata (mirrors RenderComponent contract for NM pass sorting)
  // -------------------------------------------------------------------------

  // -------------------------------------------------------------------------
  // RenderComponent
  // -------------------------------------------------------------------------

  /**
   * NM entities are drawn exclusively by {@code NormalMappedRenderSystem}. Calling this method
   * directly is a programming error.
   */
  @Override
  public void draw(PolygonSpriteBatch batch) {
    throw new UnsupportedOperationException(
        "NormalMappedRenderComponent must be drawn via NormalMappedRenderSystem");
  }

  @Override
  public RenderPass renderPass() {
    return RenderPass.NORMAL_MAPPED;
  }

  /** World-Y bounding-box bottom edge, used for painter's-algorithm sorting. */
  @Override
  public float sortKey() {
    return instance.bounds(boundsCache).y;
  }

  // -------------------------------------------------------------------------
  // Direct access for WorldEntityLayer
  // -------------------------------------------------------------------------

  public NormalMappedDrawable instance() {
    return instance;
  }
}
