package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.render.OutlineSource;

/**
 * UI-widget specialization of {@link OutlineSource} for widgets that need per-frame FBO outline
 * capture and blit (e.g. texture hotspots drawn inside panels).
 *
 * <p>It feeds the same {@link com.cryptroot.core.render.SelectionOutlineRenderer} pipeline as world
 * entities — sharing the single {@link OutlineSource} contract ({@code outlineActive} / {@code
 * outlineAlpha} / {@code drawForCapture}) — but adds a UI-only {@link #drawPostOutline} hook for
 * content (e.g. hover labels) that must render on top of the outline ring.
 *
 * <h3>Integration with {@link UiLayer}</h3>
 *
 * <ol>
 *   <li>Call {@link UiLayer#captureOutlines} <em>before</em> {@code batch.begin()} for the current
 *       frame — this renders each active source's pixels into the shared FBO.
 *   <li>After drawing all widgets normally, call {@link UiLayer#drawOutlines} <em>inside</em>
 *       {@code batch.begin()/end()} to blit the outline over the scene.
 *   <li>Optionally call {@link UiLayer#drawPostOutlines} immediately after to draw any label
 *       overlays on top of the ring.
 * </ol>
 *
 * <p><b>Note:</b> {@code SelectionOutlineRenderer} owns a single FBO. When multiple sources are
 * active simultaneously they are rendered together into that FBO in a single capture pass; the
 * outline ring then appears around every active source's opaque pixels. The maximum per-source
 * alpha is used for the ring opacity.
 */
public interface OutlineCaptureSource extends OutlineSource {

  /**
   * Called by {@link UiLayer#drawPostOutlines} immediately after the outline FBO has been blitted,
   * while the batch is still open. Override to draw any content (e.g. hover labels) that must
   * appear on top of the outline pass.
   *
   * <p>Only invoked when {@link #outlineActive()} is {@code true}.
   */
  default void drawPostOutline(PolygonSpriteBatch batch) {}
}
