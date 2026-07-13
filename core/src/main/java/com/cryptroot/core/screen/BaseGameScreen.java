package com.cryptroot.core.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.cryptroot.core.GameContext;
import com.cryptroot.core.render.RenderPipeline;
import com.cryptroot.core.time.TimeScale;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldCameraController;

/**
 * Abstract base for screens that own a {@link World} and a panning {@link WorldCameraController}.
 *
 * <p>Parameterised on {@code C} so each game supplies its own concrete {@link GameContext}
 * subclass. World dimensions are derived from the context's viewport ({@link
 * com.badlogic.gdx.utils.viewport.Viewport#getWorldWidth()} / {@link
 * com.badlogic.gdx.utils.viewport.Viewport#getWorldHeight()}) so no game-specific constants need to
 * be hard-coded here.
 *
 * <p>The standard world pipeline is sealed here so all game-world screens share the same
 * orchestration (via {@link RenderPipeline}).
 *
 * <p>Subclasses populate {@link #world} and wire input in {@code show()}.
 *
 * @param <C> the concrete {@link GameContext} subclass for this game
 */
public abstract class BaseGameScreen<C extends GameContext> extends BaseScreen<C> {

  protected final WorldCameraController worldCamera;
  protected final World world;
  protected final RenderPipeline pipeline;

  /**
   * Pause/speed-up control applied to the frame delta before it reaches {@link #world}. Defaults to
   * a 1x, unpaused identity — call its setters (typically from a key binding or a pause button) to
   * change playback speed.
   */
  protected final TimeScale timeScale = new TimeScale();

  protected BaseGameScreen(C context) {
    super(context);
    worldCamera =
        new WorldCameraController(
            context.viewport().getWorldWidth(),
            context.viewport().getWorldHeight(),
            context.viewport());
    world = new World();
    pipeline = new RenderPipeline(context);
  }

  // -------------------------------------------------------------------------
  // Rendering (sealed — subclasses configure entities; they cannot change the
  // pipeline order)
  // -------------------------------------------------------------------------

  @Override
  protected final void onRender(float delta) {
    float scaledDelta = timeScale.apply(delta);
    Vector3 mouse = worldCamera.unproject(Gdx.input.getX(), Gdx.input.getY());
    pipeline.update(world, scaledDelta);
    pipeline.processHover(world, mouse.x, mouse.y, scaledDelta);
    pipeline.processCollisions(world);
    pipeline.render(world, worldCamera.camera(), uiLayer);
  }

  // -------------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------------

  @Override
  protected void onHide() {
    worldCamera.reset();
    world.clear();
    pipeline.reset();
  }
}
