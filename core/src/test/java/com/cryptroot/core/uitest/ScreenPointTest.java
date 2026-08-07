package com.cryptroot.core.uitest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;

/**
 * Plain JUnit — no GL, no window. A bare {@link OrthographicCamera} can be built and updated
 * without a backend, so the conversion every synthetic click depends on is verified here rather
 * than only implicitly, by clicks landing.
 */
class ScreenPointTest {

  static {
    // Matrix4.prj (used by OrthographicCamera.update) is a native method; without a full
    // Application, nothing else triggers loading libgdx's shared library.
    com.badlogic.gdx.utils.GdxNativesLoader.load();
  }

  private static final int WORLD_W = 100;
  private static final int WORLD_H = 50;
  private static final int WINDOW_W = 200;
  private static final int WINDOW_H = 100;

  private static OrthographicCamera camera() {
    OrthographicCamera camera = new OrthographicCamera();
    camera.setToOrtho(false, WORLD_W, WORLD_H);
    camera.update();
    return camera;
  }

  private static Vector3 toScreen(float worldX, float worldY) {
    return ScreenPoint.toScreen(
        camera(), 0, 0, WINDOW_W, WINDOW_H, WINDOW_H, worldX, worldY, new Vector3());
  }

  @Test
  void flipIsItsOwnInverse() {
    assertEquals(30f, ScreenPoint.flipY(100, ScreenPoint.flipY(100, 30f)));
  }

  @Test
  void worldOriginIsBottomLeftOfTheScreen() {
    Vector3 screen = toScreen(0f, 0f);
    assertEquals(0f, screen.x, 0.001f);
    assertEquals(WINDOW_H, screen.y, 0.001f);
  }

  @Test
  void topRightOfTheWorldIsTopRightOfTheScreen() {
    Vector3 screen = toScreen(WORLD_W, WORLD_H);
    assertEquals(WINDOW_W, screen.x, 0.001f);
    assertEquals(0f, screen.y, 0.001f);
  }

  @Test
  void worldCentreIsScreenCentre() {
    Vector3 screen = toScreen(WORLD_W / 2f, WORLD_H / 2f);
    assertEquals(WINDOW_W / 2f, screen.x, 0.001f);
    assertEquals(WINDOW_H / 2f, screen.y, 0.001f);
  }

  @Test
  void yGrowsUpwardsInTheWorldAndDownwardsOnScreen() {
    // The bug this guards against — forgetting the flip — is invisible at the centre and only shows
    // up off-centre, where a click would land mirrored about the horizontal axis.
    Vector3 lower = toScreen(0f, WORLD_H * 0.25f);
    Vector3 upper = toScreen(0f, WORLD_H * 0.75f);
    assertEquals(75f, lower.y, 0.001f);
    assertEquals(25f, upper.y, 0.001f);
  }

  @Test
  void offsetViewportShiftsTheResult() {
    // A letter-boxed FitViewport sits inset in the window; the conversion must use its rectangle.
    Vector3 screen =
        ScreenPoint.toScreen(camera(), 20, 10, WINDOW_W, WINDOW_H, WINDOW_H, 0f, 0f, new Vector3());
    assertEquals(20f, screen.x, 0.001f);
    assertEquals(WINDOW_H - 10f, screen.y, 0.001f);
  }

  @Test
  void nonPositiveViewportIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ScreenPoint.toScreen(camera(), 0, 0, 0, WINDOW_H, WINDOW_H, 0f, 0f, new Vector3()));
  }
}
