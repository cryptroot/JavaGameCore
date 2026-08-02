package com.cryptroot.core.uitest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.cryptroot.core.debug.ScreenCapture;
import com.cryptroot.core.ui.BoundedWidget;
import com.cryptroot.core.ui.UiLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An ordered script of interactions, assertions and screenshots, run one {@link Step} per frame by
 * {@link ScenarioDriver}.
 *
 * <pre>{@code
 * UiScenario.begin()
 *     .waitFrames(3)
 *     .capture("before.png")
 *     .click("confirm the choice", layer -> WidgetQuery.requireButton(layer, "Confirm"))
 *     .waitUntil("the model accepted it", model::isConfirmed)
 *     .check("the row is rendered", () -> assertTrue(WidgetQuery.textContaining(layer, "Item 1")))
 *     .capture("after.png")
 *     .build();
 * }</pre>
 *
 * <h3>Two rules the builder enforces by shape</h3>
 *
 * <ul>
 *   <li><b>Targets are resolved late.</b> {@link #click(String, Function)} takes a resolver, not a
 *       widget, because a screen that repopulates itself replaces its widgets — see {@link
 *       WidgetQuery}.
 *   <li><b>Nothing is asserted on the frame of a click.</b> A {@code Button} fires its {@code
 *       onClick} only after its feedback delay, and the frame it fires on is <em>consumed</em>
 *       ({@code UiLayer.update} returns {@code true}, so {@code BaseScreen.render} draws nothing).
 *       So the post-click step is {@link #waitUntil(String, BooleanSupplier)} on an observable
 *       effect, never a guessed frame count, and {@link #capture(String)} settles first.
 * </ul>
 */
public final class UiScenario {

  /** Frames a {@link #waitUntil} step waits before giving up. Two seconds at 60 fps. */
  public static final int DEFAULT_WAIT_FRAMES = 120;

  /**
   * Frames {@link #capture} lets pass before reading the back buffer.
   *
   * <p>Two, not zero: a frame consumed by {@code UiLayer.update} draws nothing, so reading the
   * buffer straight after a click can capture a stale or half-cleared image.
   */
  public static final int CAPTURE_SETTLE_FRAMES = 2;

  private final List<Step> steps;

  private UiScenario(List<Step> steps) {
    this.steps = List.copyOf(steps);
  }

  /** The steps, in order. */
  public List<Step> steps() {
    return steps;
  }

  /** Starts a new scenario. */
  public static Builder begin() {
    return new Builder();
  }

  /** Fluent builder; see the class comment for an example. */
  public static final class Builder {

    private final List<Step> steps = new ArrayList<>();

    private Builder() {}

    /** Adds a bespoke step. */
    public Builder step(Step step) {
      steps.add(Objects.requireNonNull(step, "step must not be null"));
      return this;
    }

    /**
     * Renders {@code frames} frames without acting — for the initial settle, or to let an animation
     * run.
     *
     * @throws IllegalArgumentException if {@code frames} is less than 1
     */
    public Builder waitFrames(int frames) {
      if (frames < 1) {
        throw new IllegalArgumentException("frames must be at least 1, got " + frames);
      }
      return step(of("wait " + frames + " frames", (robot, frame) -> frame + 1 >= frames));
    }

    /** Runs an arbitrary action against the robot, then moves on. */
    public Builder run(String name, Consumer<UiRobot> action) {
      Objects.requireNonNull(action, "action must not be null");
      return step(
          of(
              name,
              (robot, frame) -> {
                action.accept(robot);
                return true;
              }));
    }

    /**
     * Asserts something about the current state, then moves on. Any JUnit assertion may be used
     * inside {@code assertion}; a failure is attributed to {@code name}.
     */
    public Builder check(String name, Runnable assertion) {
      Objects.requireNonNull(assertion, "assertion must not be null");
      return step(
          of(
              name,
              (robot, frame) -> {
                assertion.run();
                return true;
              }));
    }

    /**
     * Clicks the widget the resolver finds in the layer <em>at the moment the step runs</em>.
     *
     * <p>Pair it with a {@link #waitUntil} on whatever the click should cause; the click itself
     * only arms the button's feedback timer.
     */
    public Builder click(String name, Function<UiLayer, ? extends BoundedWidget> resolver) {
      Objects.requireNonNull(resolver, "resolver must not be null");
      return run(name, robot -> robot.click(resolver.apply(robot.layer())));
    }

    /**
     * Moves the pointer over the resolved widget without clicking, so the next frame shows hover.
     */
    public Builder hover(String name, Function<UiLayer, ? extends BoundedWidget> resolver) {
      Objects.requireNonNull(resolver, "resolver must not be null");
      return run(name, robot -> robot.hover(resolver.apply(robot.layer())));
    }

    /** Waits for {@code condition}, up to {@link #DEFAULT_WAIT_FRAMES} frames. */
    public Builder waitUntil(String name, BooleanSupplier condition) {
      return waitUntil(name, condition, DEFAULT_WAIT_FRAMES);
    }

    /**
     * Waits for {@code condition}, failing after {@code maxFrames}.
     *
     * @throws IllegalArgumentException if {@code maxFrames} is less than 1
     */
    public Builder waitUntil(String name, BooleanSupplier condition, int maxFrames) {
      Objects.requireNonNull(condition, "condition must not be null");
      if (maxFrames < 1) {
        throw new IllegalArgumentException("maxFrames must be at least 1, got " + maxFrames);
      }
      return step(
          of(
              name,
              (robot, frame) -> {
                if (condition.getAsBoolean()) return true;
                if (frame + 1 >= maxFrames) {
                  throw new AssertionError(
                      "timed out after " + maxFrames + " frames waiting for: " + name);
                }
                return false;
              }));
    }

    /**
     * Repeats {@code action} every {@code everyFrames} frames until {@code condition} holds — for
     * "press Step until the shift is over".
     *
     * <p>The pacing exists because a button click is not instantaneous: {@code onClick} fires a few
     * frames later, so clicking every frame would queue up presses the screen has not processed
     * yet.
     *
     * @param maxIterations fails the scenario if the condition still does not hold after this many
     *     actions, so a broken screen ends the run instead of hanging it
     */
    public Builder repeatUntil(
        String name,
        BooleanSupplier condition,
        Consumer<UiRobot> action,
        int everyFrames,
        int maxIterations) {
      Objects.requireNonNull(condition, "condition must not be null");
      Objects.requireNonNull(action, "action must not be null");
      if (everyFrames < 1) {
        throw new IllegalArgumentException("everyFrames must be at least 1, got " + everyFrames);
      }
      if (maxIterations < 1) {
        throw new IllegalArgumentException(
            "maxIterations must be at least 1, got " + maxIterations);
      }
      return step(
          of(
              name,
              (robot, frame) -> {
                if (condition.getAsBoolean()) return true;
                if (frame % everyFrames != 0) return false;
                int iteration = frame / everyFrames;
                if (iteration >= maxIterations) {
                  throw new AssertionError(
                      "gave up after " + maxIterations + " attempts at: " + name);
                }
                action.accept(robot);
                return false;
              }));
    }

    /**
     * Writes the current frame to {@code fileName} under the capture directory (see {@link
     * UiTestConfig#captureDir()}), after {@link #CAPTURE_SETTLE_FRAMES} settle frames.
     *
     * <p>The capture is taken by {@link ScreenCapture}, i.e. from the back buffer via {@code
     * glReadPixels}, and not by an OS screen grab — which is both what makes it work on Wayland
     * without a screencast permission and why the window need not be focused or even visible.
     */
    public Builder capture(String fileName) {
      Objects.requireNonNull(fileName, "fileName must not be null");
      return step(
          of(
              "capture " + fileName,
              (robot, frame) -> {
                if (frame < CAPTURE_SETTLE_FRAMES) return false;
                FileHandle written = ScreenCapture.capture(UiTestApp.capturePath(fileName));
                Gdx.app.log("UiScenario", "captured " + written.file().getAbsolutePath());
                return true;
              }));
    }

    /** Builds the scenario. */
    public UiScenario build() {
      if (steps.isEmpty()) {
        throw new IllegalStateException("a scenario must have at least one step");
      }
      return new UiScenario(steps);
    }

    private static Step of(String name, Ticker ticker) {
      Objects.requireNonNull(name, "name must not be null");
      return new Step() {
        @Override
        public String name() {
          return name;
        }

        @Override
        public boolean tick(UiRobot robot, int frame) {
          return ticker.tick(robot, frame);
        }
      };
    }

    /** The lambda shape behind {@link Step#tick(UiRobot, int)}. */
    private interface Ticker {
      boolean tick(UiRobot robot, int frame);
    }
  }
}
