package com.cryptroot.core.render;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.Objects;

/**
 * The pan/zoom mapping between a scene's own coordinate space and a rectangle of the surface it is
 * drawn on — a camera expressed as a {@link Matrix4} the caller composes into a batch, rather than
 * as an {@code OrthographicCamera} that owns a whole screen.
 *
 * <p>This is the piece that lets a scene be drawn <em>inside</em> something else. An {@link
 * com.badlogic.gdx.graphics.OrthographicCamera} maps a scene to the entire render target via the
 * projection matrix; there can only usefully be one of those active at a time, and swapping it
 * mid-pass breaks every contract that asserts on the projection. A {@code SceneTransform} instead
 * produces a <em>transform</em> matrix that composes with whatever projection is already active, so
 * the enclosing coordinate space (a UI layer, a parent scene) is preserved and any number of scenes
 * can be nested inside it. {@link com.cryptroot.core.ui.WorldViewport} is the widget built on this;
 * games that need a scene in a box should use that rather than driving this directly.
 *
 * <h3>State</h3>
 *
 * The view rectangle ({@link #setView}) is the destination, in the enclosing space's coordinates.
 * The centre ({@link #centreX()}, {@link #centreY()}) is the scene coordinate drawn at the middle
 * of that rectangle, and {@link #zoom()} is the scale factor applied to scene units.
 *
 * <p><b>Zoom direction:</b> larger means <em>magnified</em> — {@code zoom == 2} draws the scene at
 * twice its natural size and shows half as much of it. This is the opposite of {@link
 * com.badlogic.gdx.graphics.OrthographicCamera#zoom}, where larger means further away; the
 * convention here matches what "zoom in" means to a player, and the two are simply reciprocals.
 *
 * <h3>Clamping</h3>
 *
 * With {@linkplain #setSceneBounds scene bounds} set, every mutation re-clamps so the scene can
 * never be dragged fully out of view. Along an axis where the visible extent exceeds the scene's
 * own, the scene is <em>centred</em> rather than pinned to one edge — pinning reads as a
 * positioning bug, whereas centring reads as "there is nothing more to show".
 *
 * <p>All maths here is pure and GL-free, so it is unit-testable without a render context; {@link
 * #clampCentre} and {@link #sceneToView(float, float, float, float)} are exposed as statics for
 * exactly that reason.
 */
public final class SceneTransform {

  /** Destination rectangle in the enclosing space's coordinates. */
  private final Rectangle view = new Rectangle();

  /** Scene extents used for clamping; only consulted while {@link #bounded}. */
  private final Rectangle sceneBounds = new Rectangle();

  private final Rectangle visibleScratch = new Rectangle();

  private boolean bounded;

  private float centreX;
  private float centreY;
  private float zoom = 1f;
  private float minZoom = 0.05f;
  private float maxZoom = 20f;

  // -------------------------------------------------------------------------
  // View rectangle
  // -------------------------------------------------------------------------

  /**
   * Sets the destination rectangle, in the coordinates of whatever space this transform is composed
   * into, then re-clamps.
   */
  public void setView(float x, float y, float width, float height) {
    view.set(x, y, width, height);
    clamp();
  }

  /** A copy of the destination rectangle. */
  public Rectangle view(Rectangle out) {
    Objects.requireNonNull(out, "out must not be null");
    return out.set(view);
  }

  // -------------------------------------------------------------------------
  // Scene bounds
  // -------------------------------------------------------------------------

  /**
   * Constrains panning and zooming to the given scene extents.
   *
   * @throws IllegalArgumentException if either axis has {@code max < min}
   */
  public void setSceneBounds(float minX, float minY, float maxX, float maxY) {
    if (maxX < minX || maxY < minY) {
      throw new IllegalArgumentException(
          "scene bounds must have max >= min, got minX="
              + minX
              + " maxX="
              + maxX
              + " minY="
              + minY
              + " maxY="
              + maxY);
    }
    sceneBounds.set(minX, minY, maxX - minX, maxY - minY);
    bounded = true;
    clamp();
  }

  /** Removes any scene bounds, allowing unlimited panning. */
  public void clearSceneBounds() {
    bounded = false;
  }

  /** Whether {@linkplain #setSceneBounds scene bounds} are currently applied. */
  public boolean isBounded() {
    return bounded;
  }

  // -------------------------------------------------------------------------
  // Zoom
  // -------------------------------------------------------------------------

  /** The current scale factor applied to scene units; larger is more magnified. */
  public float zoom() {
    return zoom;
  }

  /**
   * Sets the zoom about the centre of the view, clamped to the {@linkplain #setZoomRange range}.
   */
  public void setZoom(float newZoom) {
    zoom = MathUtils.clamp(newZoom, minZoom, maxZoom);
    clamp();
  }

  /**
   * Multiplies the zoom while keeping the scene point currently under ({@code anchorViewX}, {@code
   * anchorViewY}) pinned there — "zoom towards the cursor", the behaviour every map and node-graph
   * editor has and the reason this is not simply {@code setZoom(zoom * factor)}.
   *
   * @param factor multiplier applied to the current zoom
   * @param anchorViewX view-space X to keep fixed
   * @param anchorViewY view-space Y to keep fixed
   */
  public void zoomAt(float factor, float anchorViewX, float anchorViewY) {
    float anchorSceneX = viewToSceneX(anchorViewX);
    float anchorSceneY = viewToSceneY(anchorViewY);
    zoom = MathUtils.clamp(zoom * factor, minZoom, maxZoom);
    // Re-derive the centre so the anchor lands back where it was under the new zoom.
    centreX = anchorSceneX - (anchorViewX - view.x - view.width / 2f) / zoom;
    centreY = anchorSceneY - (anchorViewY - view.y - view.height / 2f) / zoom;
    clamp();
  }

  /**
   * Sets the permitted zoom range and re-clamps the current zoom into it.
   *
   * @throws IllegalArgumentException if {@code min} is not positive, or {@code max < min}
   */
  public void setZoomRange(float min, float max) {
    if (min <= 0f || max < min) {
      throw new IllegalArgumentException(
          "zoom range must satisfy 0 < min <= max, got min=" + min + " max=" + max);
    }
    minZoom = min;
    maxZoom = max;
    setZoom(zoom);
  }

  public float minZoom() {
    return minZoom;
  }

  public float maxZoom() {
    return maxZoom;
  }

  // -------------------------------------------------------------------------
  // Pan
  // -------------------------------------------------------------------------

  /** The scene X drawn at the horizontal middle of the view rectangle. */
  public float centreX() {
    return centreX;
  }

  /** The scene Y drawn at the vertical middle of the view rectangle. */
  public float centreY() {
    return centreY;
  }

  /** Pans so scene point ({@code sceneX}, {@code sceneY}) is drawn at the view's centre. */
  public void centreOn(float sceneX, float sceneY) {
    centreX = sceneX;
    centreY = sceneY;
    clamp();
  }

  /**
   * Drags the scene by a view-space delta — the content follows the pointer, so a positive {@code
   * dxView} moves the scene right and the centre left. Zoom-aware: at 2× zoom the same pointer
   * travel covers half as much scene.
   */
  public void panByView(float dxView, float dyView) {
    centreX -= dxView / zoom;
    centreY -= dyView / zoom;
    clamp();
  }

  // -------------------------------------------------------------------------
  // Conversions
  // -------------------------------------------------------------------------

  public float sceneToViewX(float sceneX) {
    return sceneToView(sceneX, centreX, view.x + view.width / 2f, zoom);
  }

  public float sceneToViewY(float sceneY) {
    return sceneToView(sceneY, centreY, view.y + view.height / 2f, zoom);
  }

  public float viewToSceneX(float viewX) {
    return viewToScene(viewX, centreX, view.x + view.width / 2f, zoom);
  }

  public float viewToSceneY(float viewY) {
    return viewToScene(viewY, centreY, view.y + view.height / 2f, zoom);
  }

  /** Converts a scene point into the enclosing space's coordinates. Returns {@code out}. */
  public Vector2 sceneToView(float sceneX, float sceneY, Vector2 out) {
    Objects.requireNonNull(out, "out must not be null");
    return out.set(sceneToViewX(sceneX), sceneToViewY(sceneY));
  }

  /** Converts a point in the enclosing space into scene coordinates. Returns {@code out}. */
  public Vector2 viewToScene(float viewX, float viewY, Vector2 out) {
    Objects.requireNonNull(out, "out must not be null");
    return out.set(viewToSceneX(viewX), viewToSceneY(viewY));
  }

  /**
   * The rectangle of scene currently visible through the view, in scene coordinates. Useful for
   * culling. Returns {@code out}.
   */
  public Rectangle visibleSceneRect(Rectangle out) {
    Objects.requireNonNull(out, "out must not be null");
    float w = view.width / zoom;
    float h = view.height / zoom;
    return out.set(centreX - w / 2f, centreY - h / 2f, w, h);
  }

  /** The visible scene rectangle, into an internal scratch instance. Do not retain the result. */
  public Rectangle visibleSceneRect() {
    return visibleSceneRect(visibleScratch);
  }

  // -------------------------------------------------------------------------
  // Matrix
  // -------------------------------------------------------------------------

  /**
   * Writes {@code base} composed with this transform into {@code out}, so that geometry submitted
   * in scene coordinates lands inside the view rectangle at the current pan and zoom.
   *
   * <p>{@code base} is the transform already active on the target batch — pass it rather than
   * assuming identity, so that a scene nested inside another transformed scene composes instead of
   * escaping its parent.
   *
   * @return {@code out}, for chaining
   */
  public Matrix4 applyTo(Matrix4 out, Matrix4 base) {
    Objects.requireNonNull(out, "out must not be null");
    Objects.requireNonNull(base, "base must not be null");
    float tx = view.x + view.width / 2f - centreX * zoom;
    float ty = view.y + view.height / 2f - centreY * zoom;
    return out.set(base).translate(tx, ty, 0f).scale(zoom, zoom, 1f);
  }

  // -------------------------------------------------------------------------
  // Clamping
  // -------------------------------------------------------------------------

  /** Re-applies the scene bounds to the current centre. No-op when unbounded. */
  public void clamp() {
    if (!bounded) return;
    centreX =
        clampCentre(centreX, sceneBounds.x, sceneBounds.x + sceneBounds.width, view.width / zoom);
    centreY =
        clampCentre(centreY, sceneBounds.y, sceneBounds.y + sceneBounds.height, view.height / zoom);
  }

  // -------------------------------------------------------------------------
  // Pure static maths — unit-testable without GL
  // -------------------------------------------------------------------------

  /**
   * Clamps a view centre so the visible window stays within {@code [sceneMin, sceneMax]} where
   * possible.
   *
   * <p>When the visible extent exceeds the scene's own along this axis, the scene cannot fill the
   * view no matter where it is placed, so it is centred with equal overhang on both sides.
   *
   * @param centre the requested scene coordinate at the middle of the view
   * @param sceneMin lower scene-coordinate extent
   * @param sceneMax upper scene-coordinate extent
   * @param visibleExtent how much scene the view shows along this axis
   * @return the clamped centre
   */
  public static float clampCentre(
      float centre, float sceneMin, float sceneMax, float visibleExtent) {
    float sceneExtent = sceneMax - sceneMin;
    if (sceneExtent <= visibleExtent) {
      return sceneMin + sceneExtent / 2f;
    }
    return MathUtils.clamp(centre, sceneMin + visibleExtent / 2f, sceneMax - visibleExtent / 2f);
  }

  /**
   * Converts one axis of a scene coordinate to view space: the scene point {@code centre} is drawn
   * at {@code viewCentre}, and scene units are scaled by {@code zoom}.
   */
  public static float sceneToView(float sceneCoord, float centre, float viewCentre, float zoom) {
    return (sceneCoord - centre) * zoom + viewCentre;
  }

  /** The inverse of {@link #sceneToView(float, float, float, float)}. */
  public static float viewToScene(float viewCoord, float centre, float viewCentre, float zoom) {
    return (viewCoord - viewCentre) / zoom + centre;
  }
}
