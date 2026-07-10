package com.cryptroot.core.render.system;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.render.SelectionOutlineRenderer;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws {@link RenderPass#BACKGROUND}, {@link RenderPass#WORLD}, and {@link
 * RenderPass#FOREGROUND_WORLD} entities.
 *
 * <p>A pre-allocated sort buffer is reused across frames to avoid per-frame allocation during
 * Y-sorting.
 */
public final class WorldRenderSystem {

  private final List<WorldEntity> sortBuffer = new ArrayList<>();

  /**
   * Draws BACKGROUND (insertion order), WORLD (Y-sorted), and the hover outline blit.
   *
   * <p>Must be called inside an active {@code batch.begin()/end()} block with the world-camera
   * projection already applied to the batch.
   */
  public void draw(
      World world,
      PolygonSpriteBatch batch,
      SelectionOutlineRenderer sor,
      OrthographicCamera worldCamera,
      HoverSystem hoverSystem) {
    drawBackground(world, batch);
    drawWorldSorted(world, batch);

    // ---- Outline blit (camera-relative, zoom-aware) ----
    float alpha = hoverSystem.hoverAlpha();
    if (alpha > 0f && hoverSystem.outlineTarget() != null) {
      blitOutline(batch, sor, worldCamera, alpha);
    }
  }

  /** Draws {@link RenderPass#BACKGROUND} entities in insertion order. */
  public void drawBackground(World world, PolygonSpriteBatch batch) {
    for (WorldEntity e : world.entities()) {
      e.get(RenderComponent.class)
          .filter(rc -> rc.renderPass() == RenderPass.BACKGROUND)
          .ifPresent(rc -> rc.draw(batch));
    }
  }

  /**
   * Draws {@link RenderPass#WORLD} entities Y-sorted ascending by {@link
   * RenderComponent#sortKey()}.
   */
  public void drawWorldSorted(World world, PolygonSpriteBatch batch) {
    sortBuffer.clear();
    for (WorldEntity e : world.entities()) {
      if (e.has(RenderComponent.class)
          && e.get(RenderComponent.class).get().renderPass() == RenderPass.WORLD) {
        sortBuffer.add(e);
      }
    }
    sortBuffer.sort(Comparator.comparingDouble(e -> e.get(RenderComponent.class).get().sortKey()));
    for (WorldEntity e : sortBuffer) {
      e.get(RenderComponent.class).ifPresent(rc -> rc.draw(batch));
    }
  }

  /**
   * Draws FOREGROUND_WORLD entities in insertion order. Must be called inside an active {@code
   * batch.begin()/end()} block.
   */
  public void drawForeground(World world, PolygonSpriteBatch batch) {
    for (WorldEntity e : world.entities()) {
      e.get(RenderComponent.class)
          .filter(rc -> rc.renderPass() == RenderPass.FOREGROUND_WORLD)
          .ifPresent(rc -> rc.draw(batch));
    }
  }

  /**
   * Draws {@link RenderPass#UI} entities in insertion order. Most games keep UI in a {@code
   * UiLayer} and never populate this pass; it exists for self-contained world scenes (e.g.
   * low-resolution pixel games) that build their UI as world entities. Must be called inside an
   * active {@code batch.begin()/end()} block.
   */
  public void drawUi(World world, PolygonSpriteBatch batch) {
    for (WorldEntity e : world.entities()) {
      e.get(RenderComponent.class)
          .filter(rc -> rc.renderPass() == RenderPass.UI)
          .ifPresent(rc -> rc.draw(batch));
    }
  }

  /**
   * Blits the captured outline FBO over the camera's visible rectangle at the given alpha. Must be
   * called inside an active {@code batch.begin()/end()} block.
   */
  public void blitOutline(
      PolygonSpriteBatch batch,
      SelectionOutlineRenderer sor,
      OrthographicCamera worldCamera,
      float alpha) {
    float visW = worldCamera.viewportWidth * worldCamera.zoom;
    float visH = worldCamera.viewportHeight * worldCamera.zoom;
    float left = worldCamera.position.x - visW / 2f;
    float bottom = worldCamera.position.y - visH / 2f;
    sor.drawOutline(batch, alpha, left, bottom, visW, visH);
  }
}
