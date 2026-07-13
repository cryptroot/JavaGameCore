package com.cryptroot.core.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.cryptroot.core.resources.ResourceManager;
import java.util.Objects;

/**
 * Synthesises solid-colour shape {@link Texture}s — circles and rings (filled or outlined), or any
 * custom {@link ShapeMask} — on the CPU and hands them to a {@link ResourceManager} for caching and
 * disposal.
 *
 * <p>This is the framework's generic "draw a shape into a texture" primitive. Game code and other
 * framework consumers (e.g. a tower-defense range ring, a selection halo) build shape overlays
 * through this factory instead of embedding {@link Pixmap} code, and the {@link ResourceManager}
 * owns the resulting textures without any knowledge of how they were produced (it only caches and
 * disposes — see {@link ResourceManager#getOrCreateTexture}).
 *
 * <p>Solid-colour <em>rectangles</em> and lines do not need this factory: draw the {@link
 * ResourceManager#getPixelTexture() 1×1 white pixel} scaled and tinted instead. This factory exists
 * for shapes the 1×1 pixel cannot express — anything round or otherwise non-rectangular.
 *
 * <p>The geometry ({@link ShapeMask}) is GL-free and unit-tested; only the {@link Pixmap}/{@link
 * Texture} rasterisation touches native/GL resources and is therefore left uncovered, matching the
 * rest of the render layer.
 */
public final class ShapeTextureFactory {

  /**
   * A per-pixel coverage test for a shape rasterised into a {@code width×height} canvas.
   *
   * <p>Returns {@code true} for the pixels that belong to the shape (they are filled with the
   * requested colour) and {@code false} for the pixels that stay transparent. Implementations are
   * pure functions of the pixel coordinate and canvas size — they hold no GL state — so any shape
   * can be described and unit-tested here without a render context.
   */
  @FunctionalInterface
  public interface ShapeMask {
    /**
     * @param x pixel column, {@code 0..width-1}
     * @param y pixel row, {@code 0..height-1}
     * @param width canvas width in pixels
     * @param height canvas height in pixels
     * @return {@code true} if pixel {@code (x,y)} is part of the shape
     */
    boolean covers(int x, int y, int width, int height);
  }

  private final ResourceManager resources;

  public ShapeTextureFactory(ResourceManager resources) {
    this.resources = Objects.requireNonNull(resources, "resources must not be null");
  }

  /**
   * A ring (circle outline) of pixel {@code diameterPx} and stroke {@code thicknessPx}, drawn in
   * {@code color}. Cached by its parameters.
   */
  public Texture ring(int diameterPx, float thicknessPx, Color color) {
    String key = "shape:ring:" + diameterPx + ':' + thicknessPx + ':' + color;
    return shape(key, diameterPx, diameterPx, color, ringMask(diameterPx, thicknessPx));
  }

  /**
   * A filled disc of pixel {@code diameterPx}, drawn in {@code color}. Cached by its parameters.
   */
  public Texture filledCircle(int diameterPx, Color color) {
    String key = "shape:disc:" + diameterPx + ':' + color;
    return shape(key, diameterPx, diameterPx, color, filledCircleMask(diameterPx));
  }

  /**
   * Rasterises an arbitrary {@link ShapeMask} into a solid-colour texture and caches it under
   * {@code key} via the {@link ResourceManager}. This is the generic entry point — {@link #ring}
   * and {@link #filledCircle} are thin wrappers over it, and any other shape can be drawn by
   * supplying a custom mask.
   *
   * @param key cache key; must be unique per distinct shape/size/colour (see {@link
   *     ResourceManager#getOrCreateTexture})
   * @param widthPx canvas width in pixels ({@code > 0})
   * @param heightPx canvas height in pixels ({@code > 0})
   * @param color fill colour (including alpha) for covered pixels
   * @param mask per-pixel coverage test
   * @return the cached-or-newly-created {@link Texture}
   */
  public Texture shape(String key, int widthPx, int heightPx, Color color, ShapeMask mask) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(color, "color must not be null");
    Objects.requireNonNull(mask, "mask must not be null");
    if (widthPx <= 0) {
      throw new IllegalArgumentException("widthPx must be positive: " + widthPx);
    }
    if (heightPx <= 0) {
      throw new IllegalArgumentException("heightPx must be positive: " + heightPx);
    }
    Color copy = new Color(color);
    return resources.getOrCreateTexture(key, () -> rasterize(widthPx, heightPx, copy, mask));
  }

  /**
   * A {@link ShapeMask} for a ring (annulus): pixels whose distance from the canvas centre lies
   * between {@code (diameterPx/2 - thicknessPx)} (clamped at 0) and {@code diameterPx/2}. A
   * thickness at or beyond the radius degenerates to a filled disc.
   */
  public static ShapeMask ringMask(int diameterPx, float thicknessPx) {
    if (diameterPx <= 0) {
      throw new IllegalArgumentException("diameterPx must be positive: " + diameterPx);
    }
    if (thicknessPx <= 0f) {
      throw new IllegalArgumentException("thicknessPx must be positive: " + thicknessPx);
    }
    float outer = diameterPx / 2f;
    float inner = Math.max(0f, outer - thicknessPx);
    float outerSq = outer * outer;
    float innerSq = inner * inner;
    return (x, y, width, height) -> {
      float dx = x + 0.5f - width / 2f;
      float dy = y + 0.5f - height / 2f;
      float distSq = dx * dx + dy * dy;
      return distSq <= outerSq && distSq >= innerSq;
    };
  }

  /** A {@link ShapeMask} for a filled disc of the given pixel diameter, centred on the canvas. */
  public static ShapeMask filledCircleMask(int diameterPx) {
    if (diameterPx <= 0) {
      throw new IllegalArgumentException("diameterPx must be positive: " + diameterPx);
    }
    float radius = diameterPx / 2f;
    float radiusSq = radius * radius;
    return (x, y, width, height) -> {
      float dx = x + 0.5f - width / 2f;
      float dy = y + 0.5f - height / 2f;
      return dx * dx + dy * dy <= radiusSq;
    };
  }

  private static Texture rasterize(int width, int height, Color color, ShapeMask mask) {
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    int rgba = Color.rgba8888(color);
    for (int py = 0; py < height; py++) {
      for (int px = 0; px < width; px++) {
        if (mask.covers(px, py, width, height)) {
          pixmap.drawPixel(px, py, rgba);
        }
      }
    }
    Texture texture = new Texture(pixmap);
    texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    pixmap.dispose();
    return texture;
  }
}
