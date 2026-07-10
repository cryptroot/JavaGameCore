package com.cryptroot.core.world;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.render.RenderPass;

/**
 * Component that contributes drawable content to its entity.
 *
 * <p>Implementations declare which {@link RenderPass} they belong to and supply a {@link
 * #sortKey()} for painter's-algorithm ordering within the {@link RenderPass#WORLD} pass. The sort
 * key is typically the entity's world Y position — a larger Y means the entity is higher on screen
 * ("closer to the viewer") and must be drawn after entities with smaller Y.
 *
 * <p>Entities in {@link RenderPass#NORMAL_MAPPED} must implement this interface but should throw
 * {@link UnsupportedOperationException} from {@link #draw} — they are rendered exclusively by
 * {@code NormalMappedRenderSystem}.
 */
public interface RenderComponent extends EntityComponent {
  void draw(PolygonSpriteBatch batch);

  RenderPass renderPass();

  /** World Y used for Y-sort within {@link RenderPass#WORLD}; ignored otherwise. */
  float sortKey();
}
