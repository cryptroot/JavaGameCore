package com.cryptroot.core.world;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.Objects;

/**
 * Encapsulates a panning {@link OrthographicCamera} for the game world and provides a reusable
 * {@link InputAdapter} for left-click-drag panning.
 *
 * <p>Screens using a world camera instantiate one controller (via the inherited {@code worldCamera}
 * field in {@link com.cryptroot.core.screen.BaseGameScreen}) and include {@link #dragAdapter()} in
 * their {@code InputMultiplexer}.
 *
 * <h3>Multiplexer ordering</h3>
 *
 * Place the drag adapter <em>after</em> any click adapters so that a click on a world entity
 * (returning {@code true} from {@code touchDown}) prevents the drag from starting on the same
 * press.
 */
public final class WorldCameraController {

  /** Minimum camera zoom (most zoomed-in; smaller value = closer). */
  public static final float ZOOM_MIN = 0.25f;

  /** Maximum camera zoom (most zoomed-out; larger value = further). */
  public static final float ZOOM_MAX = 3.0f;

  /** Multiplier applied per scroll notch. */
  public static final float ZOOM_STEP = 0.1f;

  private final OrthographicCamera camera;
  private final float worldWidth;
  private final float worldHeight;
  private final Viewport viewport;

  /** Reusable temp vector — do not hold a reference across frames. */
  private final Vector3 tmpUnproject = new Vector3();

  private boolean isDragging;
  private int lastDragScreenX;
  private int lastDragScreenY;

  /**
   * Creates a world camera centred at {@code (worldWidth/2, worldHeight/2)}.
   *
   * @param worldWidth logical world width (e.g. {@code MyJourneyGame.WORLD_WIDTH})
   * @param worldHeight logical world height
   * @param viewport the shared FitViewport used for letterbox-aware unprojection
   */
  public WorldCameraController(float worldWidth, float worldHeight, Viewport viewport) {
    Objects.requireNonNull(viewport, "viewport must not be null");
    if (worldWidth <= 0f || worldHeight <= 0f) {
      throw new IllegalArgumentException("world size must be positive");
    }
    this.worldWidth = worldWidth;
    this.worldHeight = worldHeight;
    this.viewport = viewport;

    camera = new OrthographicCamera();
    camera.setToOrtho(false, worldWidth, worldHeight);
    camera.position.set(worldWidth / 2f, worldHeight / 2f, 0f);
    camera.update();
  }

  // -------------------------------------------------------------------------
  // Accessors
  // -------------------------------------------------------------------------

  /** Returns the underlying world camera for projection matrices and {@code drawWorld()} calls. */
  public OrthographicCamera camera() {
    return camera;
  }

  // -------------------------------------------------------------------------
  // Unprojection
  // -------------------------------------------------------------------------

  /**
   * Unprojects {@code (screenX, screenY)} into world space, accounting for FitViewport
   * letterboxing. The result is written into {@code out} and returned.
   */
  public Vector3 unproject(int screenX, int screenY, Vector3 out) {
    Objects.requireNonNull(out, "out must not be null");
    out.set(screenX, screenY, 0f);
    camera.unproject(
        out,
        viewport.getScreenX(),
        viewport.getScreenY(),
        viewport.getScreenWidth(),
        viewport.getScreenHeight());
    return out;
  }

  /**
   * Convenience overload — result written into an internal reusable vector. <strong>Do not hold a
   * reference to the returned vector across frames.</strong>
   */
  public Vector3 unproject(int screenX, int screenY) {
    return unproject(screenX, screenY, tmpUnproject);
  }

  // -------------------------------------------------------------------------
  // Input adapter
  // -------------------------------------------------------------------------

  /**
   * Returns a reusable {@link InputAdapter} that handles left-click-drag panning.
   *
   * <p>The adapter consumes {@code touchDown} so that no deeper adapter starts an unrelated drag on
   * the same press. It does <em>not</em> consume {@code touchUp}, allowing other adapters to see
   * the release.
   */
  public InputAdapter dragAdapter() {
    return new InputAdapter() {
      @Override
      public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.LEFT) return false;
        isDragging = true;
        lastDragScreenX = screenX;
        lastDragScreenY = screenY;
        return true;
      }

      @Override
      public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!isDragging) return false;
        float worldPerPixelX = worldWidth / (float) viewport.getScreenWidth();
        float worldPerPixelY = worldHeight / (float) viewport.getScreenHeight();
        camera.position.x -= (screenX - lastDragScreenX) * worldPerPixelX;
        camera.position.y += (screenY - lastDragScreenY) * worldPerPixelY;
        camera.update();
        lastDragScreenX = screenX;
        lastDragScreenY = screenY;
        return true;
      }

      @Override
      public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        isDragging = false;
        return false;
      }
    };
  }

  // -------------------------------------------------------------------------
  // Zoom
  // -------------------------------------------------------------------------

  /**
   * Returns the current camera zoom (1 = 100 %, {@literal <}1 = zoomed in, {@literal >}1 = zoomed
   * out).
   */
  public float zoom() {
    return camera.zoom;
  }

  /**
   * Sets the camera zoom, clamped to [{@link #ZOOM_MIN}, {@link #ZOOM_MAX}], and updates matrices.
   *
   * @param zoom the desired zoom value
   */
  public void setZoom(float zoom) {
    camera.zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom));
    camera.update();
  }

  /**
   * Returns a reusable {@link InputAdapter} that handles mouse-wheel zoom.
   *
   * <p>Each scroll notch multiplies the current zoom by {@code (1 ± }{@link #ZOOM_STEP}{@code )},
   * keeping the camera centred. Zoom is clamped to [{@link #ZOOM_MIN}, {@link #ZOOM_MAX}].
   */
  public InputAdapter scrollAdapter() {
    return new InputAdapter() {
      @Override
      public boolean scrolled(float amountX, float amountY) {
        // amountY > 0 = scroll down = zoom out; < 0 = scroll up = zoom in.
        float factor = (amountY > 0) ? (1f + ZOOM_STEP) : (1f - ZOOM_STEP);
        setZoom(camera.zoom * factor);
        return true;
      }
    };
  }

  // -------------------------------------------------------------------------
  // Camera positioning
  // -------------------------------------------------------------------------

  /** Repositions the camera to centre on {@code (x, y)} and updates the matrices. */
  public void centreOn(float x, float y) {
    camera.position.set(x, y, 0f);
    camera.update();
  }

  // -------------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------------

  /**
   * Clears drag state and resets zoom to 1; called automatically by {@link
   * com.cryptroot.core.screen.BaseGameScreen#onHide()}.
   */
  public void reset() {
    isDragging = false;
    camera.zoom = 1f;
    camera.update();
  }
}
