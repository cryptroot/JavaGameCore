package com.cryptroot.performance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.core.FontSize;
import com.cryptroot.core.physics.CollisionSystem;
import com.cryptroot.core.render.RenderPipeline;
import com.cryptroot.core.screen.BaseScreen;
import com.cryptroot.core.ui.TextLabel;
import com.cryptroot.core.world.World;

/**
 * Visual showcase: a field of {@link BoxField}-populated boxes bouncing around a fixed arena,
 * flashing red on collision. Press {@code P} to toggle between two {@link CollisionSystem}
 * instances — one built with the sequential no-arg constructor, one built with {@link
 * CollisionSystem#CollisionSystem(com.cryptroot.core.concurrent.WorkerPool)} — to make the parallel
 * win directly visible via the HUD's collision-step timing. {@code +}/{@code -} rescale the box
 * count; below {@code CollisionSystem}'s internal parallel threshold both modes run identically (by
 * design — see {@code core.physics.CollisionSystem}), so the difference only shows up once the box
 * count is large enough.
 *
 * <p>Extends {@link BaseScreen} directly rather than {@code core.screen.BaseGameScreen} because the
 * latter's frame pipeline is sealed to a single {@code CollisionSystem} instance — this screen
 * needs to swap between two instances at runtime.
 */
public final class BoxFieldScreen extends BaseScreen<PerfDemoContext> {

  private static final int INITIAL_COUNT = 2000;
  private static final int COUNT_STEP = 500;
  private static final int MIN_COUNT = 100;
  private static final int MAX_COUNT = 20_000;

  private final World world = new World();
  private final RenderPipeline pipeline;
  private final CollisionSystem sequential = new CollisionSystem();
  private final CollisionSystem parallel;

  private TextureRegion pixel;
  private TextLabel hud;
  private boolean useParallel;
  private int boxCount = INITIAL_COUNT;
  private double lastCollisionMillis;

  public BoxFieldScreen(PerfDemoContext context) {
    super(context);
    this.pipeline = new RenderPipeline(context);
    this.parallel = new CollisionSystem(context.workerPool());
  }

  @Override
  protected Color clearColor() {
    return Color.BLACK;
  }

  @Override
  public void show() {
    pixel = new TextureRegion(context.assets().resources().getPixelTexture());
    spawnBoxes(boxCount);

    hud =
        new TextLabel(
            context.assets().font(FontSize.HINT),
            "",
            12f,
            context.viewport().getWorldHeight() - 12f);
    uiLayer.add(hud, 0);
    refreshHud();

    Gdx.input.setInputProcessor(keyAdapter());
  }

  @Override
  protected void onRender(float delta) {
    pipeline.update(world, delta);

    long start = System.nanoTime();
    activeSystem().update(world);
    lastCollisionMillis = (System.nanoTime() - start) / 1_000_000.0;

    pipeline.render(world, context.camera(), uiLayer);
    refreshHud();
  }

  @Override
  protected void onHide() {
    world.clear();
    pipeline.reset();
  }

  private CollisionSystem activeSystem() {
    return useParallel ? parallel : sequential;
  }

  private void spawnBoxes(int count) {
    world.clear();
    sequential.reset();
    parallel.reset();
    BoxField.populate(
        world,
        count,
        context.viewport().getWorldWidth(),
        context.viewport().getWorldHeight(),
        pixel);
    boxCount = count;
  }

  private void refreshHud() {
    hud.setText(
        String.format(
            "%s | boxes=%d | collision step=%.2f ms | fps=%d\n[P] toggle mode   [+/-] box count",
            useParallel ? "PARALLEL" : "SEQUENTIAL",
            boxCount,
            lastCollisionMillis,
            Gdx.graphics.getFramesPerSecond()));
  }

  private InputAdapter keyAdapter() {
    return new InputAdapter() {
      @Override
      public boolean keyDown(int keycode) {
        switch (keycode) {
          case Input.Keys.P:
            useParallel = !useParallel;
            return true;
          case Input.Keys.PLUS:
          case Input.Keys.EQUALS:
            spawnBoxes(Math.min(MAX_COUNT, boxCount + COUNT_STEP));
            return true;
          case Input.Keys.MINUS:
            spawnBoxes(Math.max(MIN_COUNT, boxCount - COUNT_STEP));
            return true;
          default:
            return false;
        }
      }
    };
  }
}
