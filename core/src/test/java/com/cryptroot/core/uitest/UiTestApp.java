package com.cryptroot.core.uitest;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import java.util.Objects;

/**
 * Runs a {@link UiTestCase} in a real LWJGL3 window and reports the outcome to the calling test.
 *
 * <pre>{@code
 * @BeforeAll
 * static void requireDisplay() {
 *   DisplayAvailability.assumeAvailable();
 * }
 *
 * @Test
 * void playsThroughTheMainScreen() {
 *   UiTestApp.run(UiTestConfig.defaults("My game UI"), new MainScreenCase());
 * }
 * }</pre>
 *
 * <p>The call blocks for the whole run — {@link Lwjgl3Application}'s constructor is the render loop
 * — and then either returns normally or throws the failure the scenario recorded, so an ordinary
 * JUnit report shows which step failed and why.
 *
 * <h3>One run per JVM</h3>
 *
 * GLFW is initialised and terminated process-wide, and re-initialising it after a shutdown is a
 * known source of driver-dependent crashes. Rather than let that surface as an unexplained flake, a
 * second run in the same JVM fails immediately with an explanation. In practice this means <b>one
 * UI test class per module</b>; if a module ever needs two, configure Surefire with {@code
 * <reuseForks>false</reuseForks>} so each class gets its own JVM.
 *
 * <h3>Why a real window and not a headless backend</h3>
 *
 * The whole point is to verify what is drawn. libGDX's headless backend has no GL at all, so {@code
 * glReadPixels} — and therefore every screenshot — is unavailable. A real window under {@code
 * xvfb}/llvmpipe is the portable way to get pixels; see {@link DisplayAvailability}.
 */
public final class UiTestApp {

  private static boolean alreadyRan;
  private static String captureDir = UiTestConfig.defaults("unused").captureDir();

  private UiTestApp() {}

  /**
   * Resolves a capture file name against the running configuration's capture directory.
   *
   * <p>Static because a {@link Step} has no configuration in hand and threading one through every
   * step would clutter the scenario API for a value that cannot change during a run — the same
   * reason it is safe: exactly one run happens per JVM.
   */
  static String capturePath(String fileName) {
    return captureDir + "/" + fileName;
  }

  /**
   * Runs {@code testCase} to completion.
   *
   * @throws org.opentest4j.TestAbortedException if no display is available (a skip, not a failure)
   *     — unless {@code -Dui.tests.require=true} was passed
   * @throws AssertionError if a step failed or the scenario did not finish within the frame budget
   */
  public static void run(UiTestConfig config, UiTestCase testCase) {
    Objects.requireNonNull(config, "config must not be null");
    Objects.requireNonNull(testCase, "testCase must not be null");
    DisplayAvailability.assumeAvailable();
    if (alreadyRan) {
      throw new IllegalStateException(
          "a UI interaction app has already run in this JVM; GLFW cannot be safely re-initialised. "
              + "Keep one UI test class per module, or set <reuseForks>false</reuseForks>.");
    }
    alreadyRan = true;
    captureDir = config.captureDir();

    ScenarioDriver driver = new ScenarioDriver(testCase, config.frameBudget());
    new Lwjgl3Application(driver, configure(config));

    if (driver.failure() != null) {
      throw new AssertionError(
          "UI scenario failed at step '" + driver.failedStep() + "': " + driver.failure(),
          driver.failure());
    }
    if (!driver.isComplete()) {
      throw new AssertionError(
          "UI scenario ended after "
              + driver.totalFrames()
              + " frames without completing — the window was closed or the application exited early");
    }
  }

  private static Lwjgl3ApplicationConfiguration configure(UiTestConfig config) {
    Lwjgl3ApplicationConfiguration lwjgl = new Lwjgl3ApplicationConfiguration();
    lwjgl.setTitle(config.title());
    lwjgl.setWindowedMode(config.width(), config.height());
    // Stencil bits included: SelectionOutlineRenderer needs them, exactly as a game launcher sets
    // it.
    lwjgl.setBackBufferConfig(8, 8, 8, 8, 16, 8, 0);
    lwjgl.setInitialVisible(config.visible());
    lwjgl.setResizable(false);
    // Vsync off with an explicit cap: the cap keeps the per-frame delta close to the 1/60 s that
    // widget timers are written against, while vsync would tie the run to the monitor.
    lwjgl.useVsync(false);
    lwjgl.setForegroundFPS(config.foregroundFps());
    // No test needs audio, and a build agent frequently has no audio device at all.
    lwjgl.disableAudio(true);
    return lwjgl;
  }
}
