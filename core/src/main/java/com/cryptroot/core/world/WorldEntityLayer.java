package com.cryptroot.core.world;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cryptroot.core.render.NormalMappedRenderer;
import com.cryptroot.core.render.SelectionOutlineRenderer;
import com.cryptroot.core.render.system.ClickSystem;
import com.cryptroot.core.render.system.DialogueSystem;
import com.cryptroot.core.render.system.HoverSystem;
import com.cryptroot.core.render.system.NormalMappedRenderSystem;
import com.cryptroot.core.render.system.OutlineRenderSystem;
import com.cryptroot.core.render.system.UpdateSystem;
import com.cryptroot.core.render.system.WorldRenderSystem;
import java.util.Collection;
import java.util.List;

/**
 * Deprecated compatibility wrapper around the new {@link World} + system architecture.
 *
 * <p>Migrate call-sites to {@link World} + individual systems or {@link
 * com.cryptroot.core.render.RenderPipeline}.
 *
 * @deprecated Use {@link World} and {@link com.cryptroot.core.render.RenderPipeline}.
 */
@Deprecated
public final class WorldEntityLayer {

  private final World world = new World();
  private final UpdateSystem updateSystem = new UpdateSystem();
  private final HoverSystem hoverSystem = new HoverSystem();
  private final ClickSystem clickSystem = new ClickSystem();
  private final DialogueSystem dialogueSystem = new DialogueSystem();
  private final OutlineRenderSystem outlineSystem = new OutlineRenderSystem();
  private final WorldRenderSystem worldRenderSystem = new WorldRenderSystem();
  private final NormalMappedRenderSystem nmSystem = new NormalMappedRenderSystem();

  public WorldEntity add(WorldEntity entity) {
    return world.add(entity);
  }

  public void update(float delta) {
    updateSystem.update(world, delta);
  }

  public void processHover(float worldX, float worldY, float delta) {
    hoverSystem.process(world, worldX, worldY, delta);
  }

  public void captureOutline(
      PolygonSpriteBatch batch,
      Matrix4 projectionMatrix,
      Viewport viewport,
      SelectionOutlineRenderer sor) {
    outlineSystem.capture(world, batch, projectionMatrix, viewport, sor, hoverSystem);
  }

  public void drawWorld(
      PolygonSpriteBatch batch, SelectionOutlineRenderer sor, OrthographicCamera worldCamera) {
    worldRenderSystem.draw(world, batch, sor, worldCamera, hoverSystem);
  }

  public void drawNormalMapped(
      PolygonSpriteBatch batch,
      NormalMappedRenderer nsr,
      Matrix4 projectionMatrix,
      Collection<Vector3> lights) {
    nmSystem.draw(world, batch, nsr, projectionMatrix, lights);
  }

  public void drawForeground(PolygonSpriteBatch batch) {
    worldRenderSystem.drawForeground(world, batch);
  }

  public boolean handleClick(float worldX, float worldY) {
    return clickSystem
        .handleClick(world, worldX, worldY)
        .map(
            e -> {
              dialogueSystem.onEntityClicked(e);
              return true;
            })
        .orElse(false);
  }

  public List<WorldEntity> findByTag(String tag) {
    return world.findByTag(tag);
  }

  public boolean hasNormalMappedEntities() {
    return nmSystem.hasEntities(world);
  }

  public Collection<Vector3> collectLights() {
    return nmSystem.collectLights(world);
  }

  public void clear() {
    world.clear();
    hoverSystem.reset();
  }

  public void reset() {
    hoverSystem.reset();
  }
}
