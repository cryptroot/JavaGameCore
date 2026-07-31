package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.Objects;

/**
 * Clips batch drawing to a world-space rectangle.
 *
 * <p>Built on libGDX's {@link ScissorStack}, which means regions <b>nest correctly</b>: pushing a
 * region intersects it with whatever region is already active, so a scrolling list inside a clipped
 * panel clips to the overlap of the two rather than escaping its parent. It also means the GL
 * scissor state is restored to the enclosing region on {@link #end}, instead of being switched off
 * outright.
 *
 * <p>{@link #begin(PolygonSpriteBatch)} returns {@code false} when the region is empty after
 * intersection — fully scrolled out of view, or nested outside its parent. Callers can skip their
 * drawing entirely in that case, and must still not call {@link #end} (there is nothing to pop).
 *
 * <p>Each {@code begin}/{@code end} pair costs two {@code batch.flush()} calls, since the scissor
 * applies at draw time and queued geometry has to be emitted under the correct state. That is
 * inherent to clipping with a batched renderer; it is a reason to clip whole regions rather than
 * individual items.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * ScissorRegion scissor = new ScissorRegion();
 *
 * // From Clippable.setClipContext, or directly if the widget owns them:
 * scissor.setClipContext(viewport, camera);
 *
 * // In doBoundedLayout():
 * scissor.setBounds(contentX, contentY, contentW, contentH);
 *
 * // In doDraw / doAfterDraw:
 * if (scissor.begin(batch)) {
 *     // ... draw clipped content ...
 *     scissor.end(batch);
 * }
 * }</pre>
 */
public final class ScissorRegion {

  /** The clip rectangle in world coordinates. */
  private final Rectangle worldBounds = new Rectangle();

  /** Reused output for {@link ScissorStack#calculateScissors} so drawing allocates nothing. */
  private final Rectangle scissorPixels = new Rectangle();

  private Viewport viewport;
  private Camera camera;

  /** {@code true} between a successful {@link #begin} and its {@link #end}. */
  private boolean pushed;

  /** Creates a region with no clip context yet; supply one via {@link #setClipContext}. */
  public ScissorRegion() {}

  /**
   * @param viewport the scene viewport used for world-to-screen mapping
   * @param camera the camera the world coordinates are expressed in
   * @param x left edge of the clip region in world coordinates
   * @param y bottom edge of the clip region in world coordinates
   * @param w width in world coordinates
   * @param h height in world coordinates
   */
  public ScissorRegion(Viewport viewport, Camera camera, float x, float y, float w, float h) {
    setClipContext(viewport, camera);
    setBounds(x, y, w, h);
  }

  /**
   * Supplies the viewport and camera to clip against. Until this is called, {@link #begin} draws
   * unclipped rather than failing, so a widget used outside a {@link UiLayer} still renders.
   *
   * <p>Both may be {@code null} together, which clears the context and returns the region to that
   * unclipped state. Supplying exactly one is rejected: {@link #begin} cannot clip without both, so
   * a half-set context would silently disable clipping for the rest of the widget's life — content
   * spilling past its panel with nothing in the logs, which is materially harder to diagnose than
   * an exception at the call that got it wrong.
   *
   * @throws IllegalArgumentException if exactly one of {@code viewport} and {@code camera} is null
   */
  public void setClipContext(Viewport viewport, Camera camera) {
    if ((viewport == null) != (camera == null)) {
      throw new IllegalArgumentException(
          "clip context needs both a viewport and a camera, or neither; got viewport="
              + viewport
              + ", camera="
              + camera);
    }
    this.viewport = viewport;
    this.camera = camera;
  }

  /** Updates the clip rectangle in world coordinates. */
  public void setBounds(float x, float y, float w, float h) {
    worldBounds.set(x, y, w, h);
  }

  /**
   * Flushes {@code batch} and pushes this region onto the {@link ScissorStack}, intersecting it
   * with any enclosing region.
   *
   * @return {@code true} if clipping is active and the caller should draw; {@code false} if the
   *     region is empty (nothing would be visible) — in which case {@link #end} must <em>not</em>
   *     be called
   */
  public boolean begin(PolygonSpriteBatch batch) {
    Objects.requireNonNull(batch, "batch must not be null");
    // No clip context: draw unclipped rather than swallow the content. Both are tested even though
    // setClipContext keeps the pair all-or-nothing — the check costs nothing and a missed null here
    // would be an NPE inside calculateScissors, far from the caller that caused it.
    if (viewport == null || camera == null) {
      return true;
    }
    batch.flush();
    ScissorStack.calculateScissors(
        camera,
        viewport.getScreenX(),
        viewport.getScreenY(),
        viewport.getScreenWidth(),
        viewport.getScreenHeight(),
        batch.getTransformMatrix(),
        worldBounds,
        scissorPixels);
    pushed = ScissorStack.pushScissors(scissorPixels);
    return pushed;
  }

  /**
   * Flushes {@code batch} and pops this region, restoring the enclosing clip state. No-op if {@link
   * #begin} did not actually push (empty region or no clip context).
   */
  public void end(PolygonSpriteBatch batch) {
    Objects.requireNonNull(batch, "batch must not be null");
    if (!pushed) return;
    batch.flush();
    ScissorStack.popScissors();
    pushed = false;
  }
}
