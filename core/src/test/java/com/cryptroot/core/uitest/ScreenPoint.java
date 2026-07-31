package com.cryptroot.core.uitest;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.Objects;

/**
 * World coordinates → the y-down "screen" coordinates that {@link com.badlogic.gdx.InputProcessor}
 * and {@link com.badlogic.gdx.Input#getX()} speak.
 *
 * <p>This is the one piece of arithmetic in the harness that silently sends every click to the
 * wrong place when it is wrong, and it is wrong in a way no assertion on game state would catch —
 * clicks would simply miss, and a widget that "was not hit" looks identical to a widget that does
 * not exist. So it is a pure static, unit-tested without GL, exactly as {@code core/CLAUDE.md} asks
 * for render/coordinate maths.
 *
 * <h3>The two spaces</h3>
 *
 * <ul>
 *   <li>{@link Viewport#project(Vector3)} / {@link Camera#project(Vector3, float, float, float,
 *       float)} produce <b>GL window</b> coordinates: origin bottom-left, y up.
 *   <li>{@code Gdx.input} and {@code InputProcessor.touchDown/touchDragged/mouseMoved} use
 *       <b>screen</b> coordinates: origin top-left, y down.
 * </ul>
 *
 * <p>The conversion is therefore {@code screenY = windowHeight - glY}, which is the exact inverse
 * of {@link Camera#unproject(Vector3, float, float, float, float)}'s first line ({@code y =
 * Gdx.graphics.getHeight() - touchCoords.y - viewportY}) — and so of the UI layer's hit-testing. It
 * is deliberately {@code windowHeight - glY} and not {@code windowHeight - 1 - glY}: matching
 * libGDX's own off-by-one convention is what makes {@code project} and {@code unproject}
 * round-trip.
 *
 * @see com.cryptroot.core.ui.UiLayer#inputProcessor()
 */
public final class ScreenPoint {

  private ScreenPoint() {}

  /**
   * Converts a GL window y (origin bottom-left) to a screen y (origin top-left), and back — the
   * mapping is its own inverse.
   *
   * @param windowHeight the window height in the same (logical) pixels the input events use
   */
  public static float flipY(int windowHeight, float glY) {
    return windowHeight - glY;
  }

  /**
   * Projects a world point into screen coordinates, using {@code viewport}'s own screen rectangle
   * so a letter-boxed or offset viewport lands correctly.
   *
   * @param windowHeight normally {@code Gdx.graphics.getHeight()}; passed in so this stays testable
   *     without a graphics backend
   * @param out receives {@code (screenX, screenY, 0)}
   * @return {@code out}
   */
  public static Vector3 toScreen(
      Viewport viewport, int windowHeight, float worldX, float worldY, Vector3 out) {
    Objects.requireNonNull(viewport, "viewport must not be null");
    return toScreen(
        viewport.getCamera(),
        viewport.getScreenX(),
        viewport.getScreenY(),
        viewport.getScreenWidth(),
        viewport.getScreenHeight(),
        windowHeight,
        worldX,
        worldY,
        out);
  }

  /**
   * As {@link #toScreen(Viewport, int, float, float, Vector3)}, but with the viewport rectangle
   * given explicitly. This overload is what the unit test drives, because a {@link Viewport} cannot
   * be {@code update()}d without a GL context while a bare {@link Camera} can.
   *
   * @throws IllegalArgumentException if the viewport width or height is not positive
   */
  public static Vector3 toScreen(
      Camera camera,
      int viewportX,
      int viewportY,
      int viewportWidth,
      int viewportHeight,
      int windowHeight,
      float worldX,
      float worldY,
      Vector3 out) {
    Objects.requireNonNull(camera, "camera must not be null");
    Objects.requireNonNull(out, "out must not be null");
    if (viewportWidth <= 0 || viewportHeight <= 0) {
      throw new IllegalArgumentException(
          "viewport must be positive, got " + viewportWidth + "x" + viewportHeight);
    }
    out.set(worldX, worldY, 0f);
    camera.project(out, viewportX, viewportY, viewportWidth, viewportHeight);
    out.y = flipY(windowHeight, out.y);
    out.z = 0f;
    return out;
  }
}
