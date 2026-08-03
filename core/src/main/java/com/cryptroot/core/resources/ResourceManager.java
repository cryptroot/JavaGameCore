package com.cryptroot.core.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Centralised texture cache that owns the lifecycle of every {@link Texture} loaded through it.
 *
 * <h3>Loading textures</h3>
 *
 * <p>Use {@link #loadTexture(ResourcePath, String)} for the common case — it prepends the {@link
 * ResourcePath} prefix to the supplied name or sub-path and applies {@link TextureFilter#Linear} /
 * {@link TextureFilter#Linear} filtering:
 *
 * <pre>{@code
 * Texture bg    = resources.loadTexture(ResourcePath.BG, "default.png");
 * Texture slice = resources.loadTexture(ResourcePath.UI, "light_border_slice.png");
 * Texture icon  = resources.loadTexture(ResourcePath.UI, "icons/bar_quest_icon.png");
 * }</pre>
 *
 * <p>Use {@link #createTexture(String, TextureFilter, TextureFilter)} when you need an explicit
 * full classpath or non-default filters.
 *
 * <h3>Caching</h3>
 *
 * <p>The cache key is the resolved classpath {@code String}. Every call that maps to the same key
 * returns the <em>same</em> {@link Texture} instance. <strong>Cache-key collision warning:</strong>
 * if the same classpath is requested twice with different filter arguments, the <em>first</em>
 * call's filters are applied and all subsequent calls silently receive that cached instance.
 * Callers must therefore use consistent filters for any given path.
 *
 * <h3>Pixel texture</h3>
 *
 * <p>A 1×1 white {@link Texture} synthesised from a {@link Pixmap} is created at construction time
 * and held in a dedicated field separate from the cache. Retrieve it via {@link
 * #getPixelTexture()}.
 *
 * <h3>Disposal</h3>
 *
 * <p>Call {@link #dispose()} once (typically from {@link
 * com.badlogic.gdx.ApplicationListener#dispose()}) to release every owned texture. The manager must
 * not be used after disposal.
 */
public final class ResourceManager implements Disposable {

  /**
   * Cache of textures loaded from the classpath. Key: full classpath string (e.g. {@code
   * "assets/ui/light_border_slice.png"}).
   */
  private final Map<String, Texture> textureCache = new HashMap<>();

  /**
   * Cache of {@link TextureAtlas} instances loaded from the classpath. Key: full classpath string
   * of the {@code .atlas} file. Atlases are owned by this manager and disposed in {@link
   * #dispose()}.
   */
  private final Map<String, TextureAtlas> atlasCache = new HashMap<>();

  /**
   * Synthesised 1×1 white pixel texture. Kept in a separate field because it has no classpath and
   * is not loaded via {@link Gdx#files}.
   */
  private final Texture pixelTexture;

  /**
   * Lazily-loaded classpath resource index (see {@link #manifest()}). {@code null} until first
   * requested; thereafter the loaded-or-empty manifest, never reloaded.
   */
  private ResourceManifest manifest;

  // -------------------------------------------------------------------------
  // Construction
  // -------------------------------------------------------------------------

  public ResourceManager() {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    pixelTexture = new Texture(pixmap);
    pixmap.dispose();
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Returns a {@link Texture} loaded from the given classpath, applying the specified min/mag
   * filters on first load and caching the result.
   *
   * <p><b>Cache-key collision:</b> if this classpath has already been loaded with different
   * filters, the cached texture (with its original filters) is returned without modification.
   * Always use the same filters for a given classpath.
   *
   * @param classpath full classpath string, e.g. {@code "assets/ui/light_border_slice.png"}
   * @param minFilter minification filter to apply on first load
   * @param magFilter magnification filter to apply on first load
   * @return the cached-or-newly-created {@link Texture}
   */
  public Texture createTexture(String classpath, TextureFilter minFilter, TextureFilter magFilter) {
    Objects.requireNonNull(classpath, "classpath must not be null");
    Objects.requireNonNull(minFilter, "minFilter must not be null");
    Objects.requireNonNull(magFilter, "magFilter must not be null");
    return textureCache.computeIfAbsent(
        classpath,
        cp -> {
          Texture t = new Texture(Gdx.files.classpath(cp));
          t.setFilter(minFilter, magFilter);
          return t;
        });
  }

  /**
   * Convenience overload that resolves the classpath from a {@link ResourcePath} root prefix and a
   * relative name or sub-path, using {@link TextureFilter#Linear} / {@link TextureFilter#Linear}
   * filtering.
   *
   * <pre>{@code
   * // "assets/ui/icons/bar_quest_icon.png"
   * resources.loadTexture(ResourcePath.UI, "icons/bar_quest_icon.png");
   * }</pre>
   *
   * @param root relative path root (provides the directory prefix)
   * @param name filename or sub-path relative to {@code root}
   * @return the cached-or-newly-created {@link Texture}
   */
  public Texture loadTexture(ResourcePath root, String name) {
    Objects.requireNonNull(root, "root must not be null");
    Objects.requireNonNull(name, "name must not be null");
    return createTexture(root.prefix() + name, TextureFilter.Linear, TextureFilter.Linear);
  }

  /**
   * Convenience overload identical to {@link #loadTexture(ResourcePath, String)} but with explicit
   * min/mag filters.
   *
   * @param root relative path root (provides the directory prefix)
   * @param name filename or sub-path relative to {@code root}
   * @param minFilter minification filter to apply on first load
   * @param magFilter magnification filter to apply on first load
   * @return the cached-or-newly-created {@link Texture}
   */
  public Texture loadTexture(
      ResourcePath root, String name, TextureFilter minFilter, TextureFilter magFilter) {
    Objects.requireNonNull(root, "root must not be null");
    Objects.requireNonNull(name, "name must not be null");
    return createTexture(root.prefix() + name, minFilter, magFilter);
  }

  /**
   * Returns a {@link Texture} loaded from an absolute filesystem path (a file <em>outside</em> the
   * classpath), applying the given filters on first load and caching the result under the same
   * ownership as every other texture — it is disposed in {@link #dispose()} like a classpath
   * texture, so callers must never dispose it themselves.
   *
   * <p>Unlike {@link #createTexture(String, TextureFilter, TextureFilter)}, which resolves against
   * the classpath, this resolves against {@code Gdx.files.absolute(path)}. It exists for content
   * that is not packaged into the application (for example a data-driven project whose image assets
   * live in a folder chosen at runtime rather than compiled onto the classpath).
   *
   * <p>The cache key is prefixed to keep it in a namespace distinct from classpath keys, so an
   * absolute path can never collide with a classpath string.
   *
   * @param absolutePath absolute filesystem path to an image file
   * @param minFilter minification filter to apply on first load
   * @param magFilter magnification filter to apply on first load
   * @return the cached-or-newly-created {@link Texture}
   */
  public Texture loadExternalTexture(
      String absolutePath, TextureFilter minFilter, TextureFilter magFilter) {
    Objects.requireNonNull(absolutePath, "absolutePath must not be null");
    Objects.requireNonNull(minFilter, "minFilter must not be null");
    Objects.requireNonNull(magFilter, "magFilter must not be null");
    return textureCache.computeIfAbsent(
        "external:" + absolutePath,
        key -> {
          Texture texture = new Texture(Gdx.files.absolute(absolutePath));
          texture.setFilter(minFilter, magFilter);
          return texture;
        });
  }

  /**
   * Convenience overload of {@link #loadExternalTexture(String, TextureFilter, TextureFilter)}
   * using {@link TextureFilter#Linear} / {@link TextureFilter#Linear} filtering.
   *
   * @param absolutePath absolute filesystem path to an image file
   * @return the cached-or-newly-created {@link Texture}
   */
  public Texture loadExternalTexture(String absolutePath) {
    return loadExternalTexture(absolutePath, TextureFilter.Linear, TextureFilter.Linear);
  }

  /**
   * Returns a {@link TextureAtlas} loaded from the given classpath, caching the result. The atlas
   * (and its page textures) is owned by this manager and disposed in {@link #dispose()}.
   *
   * <p>Callers must <strong>not</strong> call {@code atlas.dispose()} themselves.
   *
   * @param classpath full classpath string of the {@code .atlas} file, e.g. {@code
   *     "assets/units/florence/front/florence_front.atlas"}
   * @return the cached-or-newly-created {@link TextureAtlas}
   */
  public TextureAtlas loadAtlas(String classpath) {
    Objects.requireNonNull(classpath, "classpath must not be null");
    return atlasCache.computeIfAbsent(classpath, cp -> new TextureAtlas(Gdx.files.classpath(cp)));
  }

  /**
   * Returns the shared 1×1 white pixel {@link Texture} synthesised at construction time.
   *
   * <p>Use this texture to draw solid-colour rectangles inside a {@link
   * com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch} without breaking the batch. Tint the batch
   * colour before drawing, then restore it to {@link Color#WHITE} afterwards.
   *
   * @return the 1×1 white pixel texture (never {@code null})
   */
  public Texture getPixelTexture() {
    return pixelTexture;
  }

  /**
   * Returns the cached texture for {@code key}, creating it via {@code factory} on first request
   * and owning it thereafter (disposed in {@link #dispose()}).
   *
   * <p>Unlike {@link #createTexture}, this manager has no knowledge of how the texture is produced:
   * the {@code factory} performs all creation — a CPU-rasterised {@link
   * com.badlogic.gdx.graphics.Pixmap} shape (see {@code core.render.ShapeTextureFactory}), a
   * render-to-texture result, and so on. The manager only owns caching and disposal, keeping
   * texture <em>creation</em> concerns out of the cache.
   *
   * <p>{@code key} shares the same namespace as the classpath keys used by {@link #createTexture},
   * so synthesised-texture callers must use a distinct, collision-proof prefix (e.g. {@code
   * "shape:ring:64:2:..."}) rather than a string that could also be a real classpath.
   *
   * @param key cache key, also the reuse identity across calls
   * @param factory creates the texture on first request; not invoked again once cached
   * @return the cached-or-newly-created {@link Texture}
   */
  public Texture getOrCreateTexture(String key, Supplier<Texture> factory) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(factory, "factory must not be null");
    return textureCache.computeIfAbsent(key, k -> factory.get());
  }

  /**
   * Returns {@code true} if a texture is already cached under {@code key} (from {@link
   * #createTexture}, {@link #loadTexture}, or {@link #getOrCreateTexture}), without triggering
   * creation.
   *
   * @param key the classpath or synthesised cache key to check
   */
  public boolean hasCachedTexture(String key) {
    Objects.requireNonNull(key, "key must not be null");
    return textureCache.containsKey(key);
  }

  /**
   * Returns the classpath {@link ResourceManifest} — the build-generated index that lets callers
   * <em>list</em> a resource directory (which the raw classpath cannot do).
   *
   * <p>Loaded lazily from {@link ResourceManifest#DEFAULT_MANIFEST_PATH} on first request and
   * cached thereafter. If no manifest was generated (e.g. an IDE build that skips the Maven
   * plugin), this returns {@link ResourceManifest#empty()} rather than failing, so callers such as
   * {@link FrameSequenceLoader} transparently fall back to probing.
   *
   * @return the loaded-or-empty manifest (never {@code null})
   */
  public ResourceManifest manifest() {
    if (manifest == null) {
      manifest = ResourceManifest.load(ResourceManifest.DEFAULT_MANIFEST_PATH);
    }
    return manifest;
  }

  // -------------------------------------------------------------------------
  // Static helpers
  // -------------------------------------------------------------------------

  /** Draw height that keeps {@code region}'s native aspect ratio for a fixed draw width. */
  public static float heightForWidth(TextureRegion region, float width) {
    Objects.requireNonNull(region, "region must not be null");
    return width * region.getRegionHeight() / region.getRegionWidth();
  }

  /** Draw width that keeps {@code region}'s native aspect ratio for a fixed draw height. */
  public static float widthForHeight(TextureRegion region, float height) {
    Objects.requireNonNull(region, "region must not be null");
    return height * region.getRegionWidth() / region.getRegionHeight();
  }

  /**
   * Fits {@code region} within a {@code maxWidth × maxHeight} box, preserving aspect ratio (like
   * CSS {@code object-fit: contain}). Never upscales beyond the box in either axis.
   */
  public static Vector2 fit(TextureRegion region, float maxWidth, float maxHeight) {
    Objects.requireNonNull(region, "region must not be null");
    float widthAtMaxHeight = widthForHeight(region, maxHeight);
    if (widthAtMaxHeight <= maxWidth) {
      return new Vector2(widthAtMaxHeight, maxHeight);
    }
    return new Vector2(maxWidth, heightForWidth(region, maxWidth));
  }

  // -------------------------------------------------------------------------
  // Disposable
  // -------------------------------------------------------------------------

  /** Disposes every texture owned by this manager. The manager must not be used after this call. */
  @Override
  public void dispose() {
    textureCache.values().forEach(Texture::dispose);
    textureCache.clear();
    atlasCache.values().forEach(TextureAtlas::dispose);
    atlasCache.clear();
    pixelTexture.dispose();
  }
}
