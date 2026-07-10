package com.cryptroot.core.render;

/**
 * Identifies which render pass a {@link com.cryptroot.core.world.RenderComponent} belongs to.
 *
 * <p>Passes are executed in the order declared here (ordinal order):
 *
 * <ol>
 *   <li>{@link #BACKGROUND} — tiled floor, BG scenery; drawn before world entities.
 *   <li>{@link #WORLD} — Y-sorted world-space entities (units, props).
 *   <li>{@link #NORMAL_MAPPED} — entities rendered via the normal-map shader; drawn in a separate
 *       batch pass by {@code NormalMappedRenderSystem}.
 *   <li>{@link #FOREGROUND_WORLD} — overlays drawn above world entities (speech bubbles, name
 *       labels) without Y-sorting.
 *   <li>{@link #UI} — screen-space UI drawn with the UI camera projection.
 * </ol>
 *
 * <p>Replaces the deprecated {@link com.cryptroot.core.world.RenderLayer} enum.
 */
public enum RenderPass {
  BACKGROUND,
  WORLD,
  NORMAL_MAPPED,
  FOREGROUND_WORLD,
  UI
}
