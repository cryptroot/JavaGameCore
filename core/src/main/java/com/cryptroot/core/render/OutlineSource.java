package com.cryptroot.core.render;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A single contributor to the shared selection-outline pass.
 *
 * <p>This is the one contract every outline-able thing implements — UI widgets, world entities
 * (texture or Spine), low-resolution pixel sprites. It replaced the two previously-parallel
 * contracts that mirrored each other: the UI-side {@link
 * com.cryptroot.core.ui.OutlineCaptureSource} (now a thin specialization of this interface) and a
 * since-removed texture-component contract in the world ECS.
 *
 * <p>A source is consumed by {@link SelectionOutlineRenderer#captureSources}: every {@linkplain
 * #outlineActive() active} source is drawn into the shared FBO via {@link
 * #drawForCapture(PolygonSpriteBatch)}, and the ring is then blitted by the caller at the maximum
 * {@linkplain #outlineAlpha() alpha} across the active set.
 *
 * <h3>Why {@code drawForCapture} instead of region + x/y/w/h</h3>
 *
 * The old contracts exposed a {@code TextureRegion} plus an explicit draw rect, which only
 * described a single flat sprite. Replaying the source's own draw call instead covers
 * <em>every</em> kind of drawable uniformly: a tinted hover icon, a Y-sorted world prop, a Spine
 * skeleton (PMA blend), or a nine-patch — all without the renderer knowing which it is. {@link
 * SelectionOutlineRenderer#captureWith} already restores the default blend mode after the drawer
 * runs, so PMA-blended Spine draws are safe here.
 *
 * <h3>Extension policy</h3>
 *
 * This is the <strong>single extension point</strong> for outlining across the engine — outlining
 * is already a first-class, shared rendering capability. To make something outline-able, implement
 * this interface (or, for world entities, mark them with {@link
 * com.cryptroot.core.world.AlwaysOutlinedComponent} / make them the hovered entity, which the
 * {@link com.cryptroot.core.render.system.OutlineRenderSystem} adapts to an {@code OutlineSource}
 * automatically). <strong>Never</strong> create a parallel outline component, a per-game outline
 * renderer, or a second capture contract — extend this one.
 */
public interface OutlineSource {

  /**
   * @return {@code true} while this source should contribute to the outline pass this frame (e.g. a
   *     UI widget whose hover fade alpha is non-zero). World targets selected at collection time
   *     typically return {@code true}.
   */
  boolean outlineActive();

  /**
   * @return the ring opacity (0–1) this source requests. When several sources are captured together
   *     the renderer rings them all at the maximum alpha.
   */
  float outlineAlpha();

  /**
   * Draws this source's opaque pixels into the outline FBO. Invoked inside an open batch whose
   * projection has already been set by the renderer; implementations issue their normal draw
   * call(s) and must not call {@code batch.begin()/end()}.
   */
  void drawForCapture(PolygonSpriteBatch batch);

  // -------------------------------------------------------------------------
  // Adapters
  // -------------------------------------------------------------------------

  /**
   * Wraps an arbitrary draw operation as an always-active source at the given alpha. Used to adapt
   * world entities (drawer = {@code renderComponent::draw}) and Spine targets (drawer = {@code
   * spineDrawable::draw}) at collection time, capturing the frame's current fade alpha.
   */
  static OutlineSource of(float alpha, Consumer<PolygonSpriteBatch> drawer) {
    Objects.requireNonNull(drawer, "drawer must not be null");
    return new OutlineSource() {
      @Override
      public boolean outlineActive() {
        return true;
      }

      @Override
      public float outlineAlpha() {
        return alpha;
      }

      @Override
      public void drawForCapture(PolygonSpriteBatch batch) {
        drawer.accept(batch);
      }
    };
  }
}
