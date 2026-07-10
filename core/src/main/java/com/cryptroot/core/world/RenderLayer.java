package com.cryptroot.core.world;

/**
 * Draw-pass ordering for {@link RenderComponent} instances.
 *
 * @deprecated Replaced by {@link com.cryptroot.core.render.RenderPass}.
 */
@Deprecated
public enum RenderLayer {
  BACKGROUND,
  WORLD,
  FOREGROUND
}
