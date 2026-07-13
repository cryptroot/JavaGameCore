package com.cryptroot.core.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.cryptroot.core.GameContext;
import com.cryptroot.core.physics.CollisionSystem;
import com.cryptroot.core.render.system.ClickSystem;
import com.cryptroot.core.render.system.DialogueSystem;
import com.cryptroot.core.render.system.HoverSystem;
import com.cryptroot.core.render.system.NormalMappedRenderSystem;
import com.cryptroot.core.render.system.OutlineRenderSystem;
import com.cryptroot.core.render.system.UpdateSystem;
import com.cryptroot.core.render.system.WorldRenderSystem;
import com.cryptroot.core.ui.UiLayer;
import com.cryptroot.core.world.World;
import java.util.Collection;
import java.util.Objects;

/**
 * Orchestrates the full per-frame render pipeline for a world-camera scene.
 *
 * <h3>Frame call sequence (handled internally)</h3>
 *
 * <ol>
 *   <li>{@link UpdateSystem}: tick all {@code UpdateComponent}s
 *   <li>{@link HoverSystem}: update hover state, fire enter/exit signals
 *   <li>{@link CollisionSystem}: update collider overlap state, fire enter/exit signals
 *   <li>{@link OutlineRenderSystem}: capture hover outline to FBO (before batch.begin)
 *   <li>{@link WorldRenderSystem#draw}: BACKGROUND + WORLD (Y-sorted) + outline blit
 *   <li>{@link NormalMappedRenderSystem}: NM pass (if entities present)
 *   <li>{@link WorldRenderSystem#drawForeground}: FOREGROUND_WORLD
 *   <li>UI pass ({@link UiLayer#draw})
 * </ol>
 *
 * <p>One instance per scene. Call {@link #reset()} when the screen hides.
 */
public final class RenderPipeline implements Disposable {

  private final GameContext context;
  private final UpdateSystem updateSystem = new UpdateSystem();
  private final HoverSystem hoverSystem = new HoverSystem();
  private final ClickSystem clickSystem = new ClickSystem();
  private final DialogueSystem dialogueSystem = new DialogueSystem();
  private final CollisionSystem collisionSystem = new CollisionSystem();
  private final OutlineRenderSystem outlineSystem = new OutlineRenderSystem();
  private final WorldRenderSystem worldRenderSystem = new WorldRenderSystem();
  private final NormalMappedRenderSystem nmRenderSystem = new NormalMappedRenderSystem();

  /**
   * Whether {@link #captureOutlines} found targets this frame (drives the {@link #renderScene}
   * blit).
   */
  private boolean sceneOutlineCaptured;

  public RenderPipeline(GameContext context) {
    this.context = Objects.requireNonNull(context, "context must not be null");
  }

  // -------------------------------------------------------------------------
  // Per-frame
  // -------------------------------------------------------------------------

  /**
   * Ticks all {@code UpdateComponent}s in the world.
   *
   * <p>Applies any {@link World#queueAdd queued} entity additions and {@link World#queueRemove
   * queued} removals first, at the start of the frame while no system is iterating, so components
   * may safely spawn or remove entities during {@code update()}.
   */
  public void update(World world, float delta) {
    world.flushAdditions();
    world.flushRemovals();
    updateSystem.update(world, delta);
  }

  /**
   * Advances hover state for the given world-space cursor position.
   *
   * @param worldX world-space cursor X (unproject from screen before calling)
   * @param worldY world-space cursor Y
   */
  public void processHover(World world, float worldX, float worldY, float delta) {
    hoverSystem.process(world, worldX, worldY, delta);
  }

  /**
   * Re-scans every {@link com.cryptroot.core.physics.Collider}-carrying entity in {@code world} and
   * fires {@link com.cryptroot.core.physics.CollisionListener} enter/exit transitions.
   *
   * <p>Cheap to call unconditionally even when no entity carries a {@link
   * com.cryptroot.core.physics.Collider} (a single filtering pass over {@link World#entities()}) —
   * a game opts in simply by attaching {@link com.cryptroot.core.physics.Collider} components, with
   * no separate wiring.
   */
  public void processCollisions(World world) {
    collisionSystem.update(world);
  }

  /**
   * Executes the full render pass: FBO capture → world draw → NM pass (if needed) → foreground →
   * UI.
   *
   * @param worldCamera the active world camera (may differ from the UI camera)
   * @param uiLayer the UI layer to draw after the world pass
   */
  public void render(World world, OrthographicCamera worldCamera, UiLayer uiLayer) {
    PolygonSpriteBatch batch = context.batch();
    SelectionOutlineRenderer sor = context.outlineRenderer();

    // FBO capture — must precede batch.begin()
    outlineSystem.capture(world, batch, worldCamera.combined, context.viewport(), sor, hoverSystem);

    // World pass
    batch.setProjectionMatrix(worldCamera.combined);
    batch.begin();
    worldRenderSystem.draw(world, batch, sor, worldCamera, hoverSystem);
    worldRenderSystem.drawForeground(world, batch);
    batch.end();

    // Normal-mapped pass
    if (nmRenderSystem.hasEntities(world)) {
      Collection<Vector3> lights = nmRenderSystem.collectLights(world);
      nmRenderSystem.draw(
          world, batch, context.normalMappedRenderer(), worldCamera.combined, lights);
    }

    // UI pass
    batch.setProjectionMatrix(context.camera().combined);
    batch.begin();
    uiLayer.draw(batch);
    batch.end();
  }

  /**
   * Performs a hit-test click, fires {@link
   * com.cryptroot.core.world.ClickableComponent#onClicked()}, and auto-starts dialogue if a {@link
   * com.cryptroot.core.world.DialogueSpeakerComponent} is present.
   *
   * @return {@code true} if an entity was hit
   */
  public boolean handleClick(World world, float worldX, float worldY) {
    Objects.requireNonNull(world, "world must not be null");
    return clickSystem
        .handleClick(world, worldX, worldY)
        .map(
            e -> {
              dialogueSystem.onEntityClicked(e);
              return true;
            })
        .orElse(false);
  }

  // -------------------------------------------------------------------------
  // Externally-managed render target (e.g. low-resolution pixel framebuffer)
  // -------------------------------------------------------------------------

  /**
   * Captures the active outline targets (all {@link
   * com.cryptroot.core.world.AlwaysOutlinedComponent} carriers plus the current hovered entity)
   * into the outline FBO.
   *
   * <p>For callers that own their own render target (e.g. a low-resolution framebuffer): call this
   * <em>before</em> binding that target, then call {@link #renderScene} after binding it. This uses
   * the always-on / instant outline path rather than the faded hover path used by {@link #render}.
   *
   * @param projection the projection the scene is drawn with (logical space)
   */
  public void captureOutlines(World world, com.badlogic.gdx.math.Matrix4 projection) {
    sceneOutlineCaptured =
        outlineSystem.captureActive(
            world,
            context.batch(),
            projection,
            context.viewport(),
            context.outlineRenderer(),
            hoverSystem);
  }

  /**
   * Draws the full world into the currently-bound render target: BACKGROUND, WORLD (Y-sorted),
   * FOREGROUND_WORLD and UI passes, followed by the outline blit (at full opacity) for whatever
   * {@link #captureOutlines} captured this frame.
   *
   * <p>Unlike {@link #render}, this draws the world's own {@link
   * com.cryptroot.core.render.RenderPass#UI} entities (no {@link UiLayer}) and assumes the caller
   * manages the target FBO and its presentation. The outline ring is blitted last so it sits above
   * the UI. There is no normal-mapped pass.
   *
   * @param sceneCamera the orthographic camera describing the logical scene rect
   */
  public void renderScene(World world, OrthographicCamera sceneCamera) {
    PolygonSpriteBatch batch = context.batch();
    SelectionOutlineRenderer sor = context.outlineRenderer();

    batch.setProjectionMatrix(sceneCamera.combined);
    batch.begin();
    worldRenderSystem.drawBackground(world, batch);
    worldRenderSystem.drawWorldSorted(world, batch);
    worldRenderSystem.drawForeground(world, batch);
    worldRenderSystem.drawUi(world, batch);
    if (sceneOutlineCaptured) {
      worldRenderSystem.blitOutline(batch, sor, sceneCamera, 1f);
    }
    batch.end();
  }

  /** Resets hover and outline state. Call from screen {@code hide()}. */
  public void reset() {
    hoverSystem.reset();
    collisionSystem.reset();
    sceneOutlineCaptured = false;
  }

  @Override
  public void dispose() {
    // Systems hold no disposable resources.
  }
}
