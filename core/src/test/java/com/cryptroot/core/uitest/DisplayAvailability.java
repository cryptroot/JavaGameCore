package com.cryptroot.core.uitest;

import org.junit.jupiter.api.Assumptions;

/**
 * Decides whether this machine can open the GL window that {@link UiTestApp} needs.
 *
 * <p>Interaction tests render a real frame, so they need a real (or virtual) display. Rather than
 * failing everywhere a display is absent — a plain CI container, a headless build agent — {@link
 * #assumeAvailable()} skips, and a build that wants to forbid skipping opts in with {@code
 * -Dui.tests.require=true}.
 *
 * <p>Detection is deliberately environmental rather than "try it and see": creating a GLFW window
 * to find out costs a native init and, when it fails, aborts the JVM rather than throwing.
 *
 * <ul>
 *   <li>Windows / macOS — always considered available.
 *   <li>Linux and other Unixes — available when {@code DISPLAY} (X11, including XWayland) or {@code
 *       WAYLAND_DISPLAY} (a native Wayland session) is set. {@code xvfb-run} sets {@code DISPLAY},
 *       so a virtual framebuffer counts.
 * </ul>
 */
public final class DisplayAvailability {

  /** {@code -Dui.tests=false} skips the interaction tests regardless of the display. */
  public static final String ENABLED_PROPERTY = "ui.tests";

  /** {@code -Dui.tests.require=true} turns "no display" from a skip into a failure. */
  public static final String REQUIRE_PROPERTY = "ui.tests.require";

  private DisplayAvailability() {}

  /** Whether the interaction tests have been switched off with {@code -Dui.tests=false}. */
  public static boolean isDisabled() {
    return "false".equalsIgnoreCase(System.getProperty(ENABLED_PROPERTY));
  }

  /** Whether {@code -Dui.tests.require=true} was passed, forbidding a skip. */
  public static boolean isRequired() {
    return Boolean.getBoolean(REQUIRE_PROPERTY);
  }

  /** Whether a GL window can be opened here. See the class comment for the per-OS rules. */
  public static boolean canOpenWindow() {
    String os = System.getProperty("os.name", "").toLowerCase();
    if (os.contains("win") || os.contains("mac") || os.contains("darwin")) {
      return true;
    }
    return isSet("DISPLAY") || isSet("WAYLAND_DISPLAY");
  }

  /** A human-readable explanation of the current verdict, used in skip and failure messages. */
  public static String describe() {
    if (isDisabled()) {
      return "-D" + ENABLED_PROPERTY + "=false was passed";
    }
    if (canOpenWindow()) {
      return "display available (os.name="
          + System.getProperty("os.name")
          + ", DISPLAY="
          + orNone(System.getenv("DISPLAY"))
          + ", WAYLAND_DISPLAY="
          + orNone(System.getenv("WAYLAND_DISPLAY"))
          + ")";
    }
    return "no DISPLAY or WAYLAND_DISPLAY set — run under a desktop session or `xvfb-run -a mvn "
        + "test` (add LIBGL_ALWAYS_SOFTWARE=1 for llvmpipe)";
  }

  /**
   * Skips the calling test when no window can be opened, or fails it when {@code
   * -Dui.tests.require=true} was passed.
   *
   * <p>Call from {@code @BeforeAll} rather than using JUnit's {@code @EnabledIf}: the annotation
   * can only disable a test, and the required mode needs the opposite outcome.
   */
  public static void assumeAvailable() {
    if (isDisabled()) {
      Assumptions.abort("UI interaction tests disabled: " + describe());
    }
    if (!canOpenWindow()) {
      if (isRequired()) {
        throw new IllegalStateException(
            "-D" + REQUIRE_PROPERTY + "=true but " + describe() + "; refusing to skip");
      }
      Assumptions.abort("Skipping UI interaction test: " + describe());
    }
  }

  private static boolean isSet(String name) {
    String value = System.getenv(name);
    return value != null && !value.isBlank();
  }

  private static String orNone(String value) {
    return value == null || value.isBlank() ? "<unset>" : value;
  }
}
