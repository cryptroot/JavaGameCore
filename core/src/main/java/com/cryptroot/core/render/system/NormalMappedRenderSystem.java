package com.cryptroot.core.render.system;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.cryptroot.core.render.NormalMappedRenderer;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.LightSourceComponent;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.component.NormalMappedRenderComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Renders {@link RenderPass#NORMAL_MAPPED} entities using the {@link NormalMappedRenderer}.
 *
 * <p>A pre-allocated sort buffer is reused across frames to avoid per-frame allocation during
 * Y-sorting.
 */
public final class NormalMappedRenderSystem {

  private final List<WorldEntity> sortBuffer = new ArrayList<>();

  /** Returns {@code true} if any entity in {@code world} requires the NM pass. */
  public boolean hasEntities(World world) {
    for (WorldEntity e : world.entities()) {
      if (e.has(RenderComponent.class)
          && e.get(RenderComponent.class).get().renderPass() == RenderPass.NORMAL_MAPPED) {
        return true;
      }
    }
    return false;
  }

  /** Collects light positions from all entities with a {@link LightSourceComponent}. */
  public Collection<Vector3> collectLights(World world) {
    List<Vector3> lights = new ArrayList<>();
    for (WorldEntity e : world.entities()) {
      e.get(LightSourceComponent.class).ifPresent(l -> lights.add(l.lightPosition()));
    }
    return lights;
  }

  /**
   * Y-sorts and draws all NM entities. Manages its own {@link NormalMappedRenderer#begin} / {@link
   * NormalMappedRenderer#end} lifecycle — do not call inside an existing {@code
   * batch.begin()/end()} block.
   */
  public void draw(
      World world,
      PolygonSpriteBatch batch,
      NormalMappedRenderer nsr,
      Matrix4 projectionMatrix,
      Collection<Vector3> lights) {
    sortBuffer.clear();
    for (WorldEntity e : world.entities()) {
      if (e.has(NormalMappedRenderComponent.class)) {
        sortBuffer.add(e);
      }
    }
    if (sortBuffer.isEmpty()) return;

    sortBuffer.sort(
        Comparator.comparingDouble(e -> e.get(NormalMappedRenderComponent.class).get().sortKey()));

    nsr.begin(batch, projectionMatrix, lights);
    for (WorldEntity e : sortBuffer) {
      nsr.draw(batch, e.get(NormalMappedRenderComponent.class).get().instance());
    }
    nsr.end(batch);
  }
}
