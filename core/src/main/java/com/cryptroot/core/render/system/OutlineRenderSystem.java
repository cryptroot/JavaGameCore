package com.cryptroot.core.render.system;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cryptroot.core.render.OutlineSource;
import com.cryptroot.core.render.SelectionOutlineRenderer;
import com.cryptroot.core.world.AlwaysOutlinedComponent;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures the current hover-outline target into the {@link SelectionOutlineRenderer}'s FBO.
 *
 * <p><strong>Must be called before any {@code batch.begin()} for this frame.</strong>
 */
public final class OutlineRenderSystem {

  /** Reused across frames to avoid per-frame allocation when collecting always-on targets. */
  private final List<WorldEntity> activeBuffer = new ArrayList<>();

  /** Reused across frames to wrap the active targets as {@link OutlineSource}s for capture. */
  private final List<OutlineSource> sourceBuffer = new ArrayList<>();

  /**
   * @param batch the batch used for FBO capture (must not be drawing)
   * @param projMatrix world projection matrix (world camera combined)
   * @param viewport the world viewport
   * @param sor the outline renderer
   * @param hoverSystem provides the current outline target and alpha
   */
  public void capture(
      World world,
      PolygonSpriteBatch batch,
      Matrix4 projMatrix,
      Viewport viewport,
      SelectionOutlineRenderer sor,
      HoverSystem hoverSystem) {
    WorldEntity target = hoverSystem.outlineTarget();
    if (target == null || hoverSystem.hoverAlpha() <= 0f) return;

    target
        .get(RenderComponent.class)
        .ifPresent(rc -> sor.captureWith(batch, projMatrix, viewport, rc::draw));
  }

  /**
   * Captures every <em>active</em> outline target into the FBO in a single pass: all entities
   * carrying an {@link AlwaysOutlinedComponent} marker plus the current {@linkplain
   * HoverSystem#hoveredEntity() hovered} entity. Each target is rendered via its {@link
   * RenderComponent#draw}, so whatever the component currently draws (including a hover tint) is
   * exactly what gets ringed by the subsequent {@link SelectionOutlineRenderer#drawOutline} blit.
   *
   * <p>Unlike {@link #capture}, this path does not fade — it is intended for the always-on /
   * instant outline use case (e.g. low-resolution pixel scenes).
   *
   * <p><strong>Must be called before any {@code batch.begin()} for this frame.</strong>
   *
   * @return {@code true} if at least one target was captured (a blit is needed)
   */
  public boolean captureActive(
      World world,
      PolygonSpriteBatch batch,
      Matrix4 projMatrix,
      Viewport viewport,
      SelectionOutlineRenderer sor,
      HoverSystem hoverSystem) {
    List<WorldEntity> targets = collectOutlineTargets(world, hoverSystem);
    if (targets.isEmpty()) return false;

    sourceBuffer.clear();
    for (WorldEntity e : targets) {
      e.get(RenderComponent.class)
          .ifPresent(rc -> sourceBuffer.add(OutlineSource.of(1f, rc::draw)));
    }
    return sor.captureSources(batch, projMatrix, viewport, sourceBuffer) > 0f;
  }

  /**
   * Collects the entities that should be outlined this frame: every {@link AlwaysOutlinedComponent}
   * carrier plus the current hovered entity (when it is renderable and not already an always-on
   * target). GL-free, so it can be unit-tested without a render context.
   */
  public List<WorldEntity> collectOutlineTargets(World world, HoverSystem hoverSystem) {
    activeBuffer.clear();
    WorldEntity hovered = hoverSystem.hoveredEntity();
    for (WorldEntity e : world.entities()) {
      boolean always = e.has(AlwaysOutlinedComponent.class);
      boolean isHover = (e == hovered);
      if ((always || isHover) && e.has(RenderComponent.class)) {
        activeBuffer.add(e);
      }
    }
    return activeBuffer;
  }
}
