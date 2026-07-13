package com.cryptroot.core;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cryptroot.core.audio.AudioManager;
import com.cryptroot.core.event.EventBus;
import com.cryptroot.core.render.NormalMappedRenderer;
import com.cryptroot.core.render.SelectionOutlineRenderer;

/**
 * Abstract base for per-game infrastructure: the batch, viewport, camera, renderers, event bus,
 * asset registry, and audio manager.
 *
 * <p>Must be constructed after LibGDX initialisation (i.e. inside {@link
 * com.badlogic.gdx.Game#create()}). Concrete subclasses supply the world dimensions and may add
 * game-specific context on top. Dispose when the game exits.
 */
public abstract class GameContext implements Disposable {

  private final OrthographicCamera camera;
  private final Viewport viewport;
  private final PolygonSpriteBatch batch;
  private final NormalMappedRenderer normalMappedRenderer;
  private final SelectionOutlineRenderer outlineRenderer;
  private final EventBus eventBus;
  private final AssetRegistry assets;
  private final AudioManager audio;

  protected GameContext(float worldWidth, float worldHeight) {
    if (worldWidth <= 0f || worldHeight <= 0f) {
      throw new IllegalArgumentException(
          "world dimensions must be positive, got " + worldWidth + "x" + worldHeight);
    }
    camera = new OrthographicCamera();
    viewport = new FitViewport(worldWidth, worldHeight, camera);
    batch = new PolygonSpriteBatch();

    camera.setToOrtho(false, worldWidth, worldHeight);
    viewport.apply(true);

    normalMappedRenderer = new NormalMappedRenderer();
    outlineRenderer = new SelectionOutlineRenderer(worldWidth, worldHeight);

    eventBus = new EventBus();
    assets = new AssetRegistry();
    audio = new AudioManager();
  }

  // -------------------------------------------------------------------------
  // Accessors
  // -------------------------------------------------------------------------

  public OrthographicCamera camera() {
    return camera;
  }

  public Viewport viewport() {
    return viewport;
  }

  public PolygonSpriteBatch batch() {
    return batch;
  }

  public NormalMappedRenderer normalMappedRenderer() {
    return normalMappedRenderer;
  }

  public SelectionOutlineRenderer outlineRenderer() {
    return outlineRenderer;
  }

  public EventBus eventBus() {
    return eventBus;
  }

  public AssetRegistry assets() {
    return assets;
  }

  public AudioManager audio() {
    return audio;
  }

  // -------------------------------------------------------------------------
  // Disposable
  // -------------------------------------------------------------------------

  @Override
  public void dispose() {
    assets.dispose();
    audio.dispose();
    normalMappedRenderer.dispose();
    outlineRenderer.dispose();
    batch.dispose();
  }
}
