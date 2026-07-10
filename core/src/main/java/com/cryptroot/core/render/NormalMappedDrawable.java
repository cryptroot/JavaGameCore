package com.cryptroot.core.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;

/**
 * Version- and engine-agnostic contract for an object the {@link NormalMappedRenderer} can light:
 * it can draw itself, expose an optional normal map, and report its world-space bounds (via {@link
 * BoundsProvider}) for Y-sorting.
 *
 * <p>This interface keeps the shared normal-mapped lighting pipeline in {@code mj-corelib} free of
 * any Spine dependency. A Spine-backed implementation (e.g. {@code SpineDrawable}) lives in the
 * consuming game module that ships the Spine runtime; {@code mj-corelib} only depends on this small
 * abstraction.
 */
public interface NormalMappedDrawable extends BoundsProvider {

  /** Advances any internal animation by {@code delta} seconds. */
  void update(float delta);

  /** Moves the drawable so its anchor sits at world-space {@code (x, y)}. */
  void setPosition(float x, float y);

  /**
   * Draws this object into the open {@code batch}. Implementations that use pre-multiplied alpha
   * must restore standard alpha blending afterwards.
   */
  void draw(PolygonSpriteBatch batch);

  /**
   * Returns the normal-map {@link Texture} for this object, or {@code null} when it should be
   * rendered unlit (a flat fallback normal is substituted).
   */
  Texture normalMap();
}
