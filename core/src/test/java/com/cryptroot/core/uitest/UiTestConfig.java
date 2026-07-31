package com.cryptroot.core.uitest;

import java.util.Objects;

/**
 * Window and pacing settings for a {@link UiTestApp} run.
 *
 * <p>Start from {@link #defaults(String)} and override with the {@code with*} methods:
 *
 * <pre>{@code
 * UiTestConfig.defaults("My game UI").withSize(1280, 800).withCaptureDir("target/uitest/main");
 * }</pre>
 *
 * @param title window title, also used in failure messages
 * @param width window width in logical pixels
 * @param height window height in logical pixels
 * @param foregroundFps frame rate cap. Kept at a real, modest rate on purpose: widget timers are
 *     measured in seconds ({@code Button}'s click feedback is 0.08 s), so an uncapped loop would
 *     need hundreds of frames where 60 fps needs five, and every {@code waitUntil} budget would
 *     have to be re-tuned per machine.
 * @param frameBudget frames the whole scenario may take before the run fails as stuck
 * @param visible whether the window is shown. Visible by default because that is the configuration
 *     the existing screenshot path is proven on; {@code -Dui.tests.hidden=true} hides it, which is
 *     handy locally and irrelevant under {@code xvfb}.
 * @param captureDir directory for {@link UiScenario.Builder#capture(String)} output, resolved as a
 *     libGDX local path (i.e. relative to the module directory under Maven)
 */
public record UiTestConfig(
    String title,
    int width,
    int height,
    int foregroundFps,
    int frameBudget,
    boolean visible,
    String captureDir) {

  /** {@code -Dui.tests.hidden=true} opens the window without showing it. */
  public static final String HIDDEN_PROPERTY = "ui.tests.hidden";

  public UiTestConfig {
    Objects.requireNonNull(title, "title must not be null");
    Objects.requireNonNull(captureDir, "captureDir must not be null");
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("window must be positive, got " + width + "x" + height);
    }
    if (foregroundFps <= 0) {
      throw new IllegalArgumentException("foregroundFps must be positive, got " + foregroundFps);
    }
    if (frameBudget <= 0) {
      throw new IllegalArgumentException("frameBudget must be positive, got " + frameBudget);
    }
  }

  /** 1280×800 at 60 fps, a 900-frame budget, captures under {@code target/uitest}. */
  public static UiTestConfig defaults(String title) {
    return new UiTestConfig(
        title, 1280, 800, 60, 900, !Boolean.getBoolean(HIDDEN_PROPERTY), "target/uitest");
  }

  public UiTestConfig withSize(int newWidth, int newHeight) {
    return new UiTestConfig(
        title, newWidth, newHeight, foregroundFps, frameBudget, visible, captureDir);
  }

  public UiTestConfig withCaptureDir(String newCaptureDir) {
    return new UiTestConfig(
        title, width, height, foregroundFps, frameBudget, visible, newCaptureDir);
  }

  public UiTestConfig withFrameBudget(int newFrameBudget) {
    return new UiTestConfig(
        title, width, height, foregroundFps, newFrameBudget, visible, captureDir);
  }
}
