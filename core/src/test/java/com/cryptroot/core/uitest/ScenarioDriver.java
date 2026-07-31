package com.cryptroot.core.uitest;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.util.List;
import java.util.Objects;

/**
 * Wraps the game's own {@link ApplicationListener} and advances a {@link UiScenario} by one {@link
 * Step} per frame.
 *
 * <h3>Where in the frame steps run</h3>
 *
 * Immediately after the wrapped {@code render()} returns — end of frame, batch closed, before the
 * buffer swap. That position is not incidental:
 *
 * <ul>
 *   <li>it is exactly what {@link com.cryptroot.core.debug.ScreenCapture ScreenCapture} documents
 *       as its precondition ("GL current, no batch drawing"), so a capture step reads the frame
 *       that was just drawn rather than a half-built one;
 *   <li>state a step asserts on has already been through a full update+draw, so "the widget shows
 *       the new value" means the drawn frame did too.
 * </ul>
 *
 * <p>One step per frame, never several: the framework's own click feedback, hover and layout passes
 * only progress between frames, so a burst of actions inside one frame would exercise a sequence a
 * player can never produce.
 *
 * <h3>Input ownership</h3>
 *
 * {@code create()} installs a {@link SyntheticInput} over the real backend input and {@code
 * dispose()} restores it, so the scripted pointer is in place before the game's own {@code
 * create()} — and hence before the first {@code show()} — runs.
 */
final class ScenarioDriver implements ApplicationListener {

  private final UiTestCase testCase;
  private final int frameBudget;

  private ApplicationListener delegate;
  private Input originalInput;
  private SyntheticInput syntheticInput;

  private UiRobot robot;
  private List<Step> steps;

  private int stepIndex;
  private int framesInStep;
  private int totalFrames;
  private boolean complete;

  private Throwable failure;
  private String failedStep;

  ScenarioDriver(UiTestCase testCase, int frameBudget) {
    this.testCase = Objects.requireNonNull(testCase, "testCase must not be null");
    this.frameBudget = frameBudget;
  }

  /** The throwable that ended the run, or {@code null} if the scenario finished. */
  Throwable failure() {
    return failure;
  }

  /** The name of the step that threw, or {@code null}. */
  String failedStep() {
    return failedStep;
  }

  /** Whether every step ran to completion. */
  boolean isComplete() {
    return complete;
  }

  /** How many frames the run took, for the summary log. */
  int totalFrames() {
    return totalFrames;
  }

  // -------------------------------------------------------------------------
  // ApplicationListener
  // -------------------------------------------------------------------------

  @Override
  public void create() {
    originalInput = Gdx.input;
    syntheticInput = new SyntheticInput(originalInput);
    Gdx.input = syntheticInput;
    delegate = testCase.createGame();
    delegate.create();
  }

  @Override
  public void resize(int width, int height) {
    delegate.resize(width, height);
  }

  @Override
  public void render() {
    delegate.render();
    if (complete || failure != null) return;
    advance();
  }

  @Override
  public void pause() {
    delegate.pause();
  }

  @Override
  public void resume() {
    delegate.resume();
  }

  @Override
  public void dispose() {
    try {
      if (delegate != null) delegate.dispose();
    } finally {
      if (originalInput != null) Gdx.input = originalInput;
    }
  }

  // -------------------------------------------------------------------------
  // Script advance
  // -------------------------------------------------------------------------

  private void advance() {
    // Clear the one-frame justTouched flag from the previous step first: a press scripted during
    // frame N must still read as "just touched" throughout frame N+1's render, which has just run.
    syntheticInput.endFrame();

    if (steps == null && !resolveScenario()) return;

    totalFrames++;
    if (totalFrames > frameBudget) {
      fail(
          currentStepName(),
          new AssertionError(
              "scenario did not finish within "
                  + frameBudget
                  + " frames; stuck on step "
                  + (stepIndex + 1)
                  + "/"
                  + steps.size()
                  + " '"
                  + currentStepName()
                  + "'"));
      return;
    }

    Step step = steps.get(stepIndex);
    if (framesInStep == 0) {
      Gdx.app.log(
          "UiScenario", "step " + (stepIndex + 1) + "/" + steps.size() + ": " + step.name());
    }
    boolean done;
    try {
      done = step.tick(robot, framesInStep);
    } catch (Throwable t) {
      fail(step.name(), t);
      return;
    }

    if (done) {
      stepIndex++;
      framesInStep = 0;
      if (stepIndex >= steps.size()) {
        complete = true;
        Gdx.app.log("UiScenario", "scenario complete in " + totalFrames + " frames");
        Gdx.app.exit();
      }
    } else {
      framesInStep++;
    }
  }

  /**
   * Builds the robot and the step list on the first driven frame.
   *
   * <p>Deferred to here rather than done in {@code create()} so the scenario is built after the
   * first frame has been drawn: a screen's widgets are laid out by the initial {@code resize()},
   * and a resolver that ran earlier would measure empty rectangles.
   *
   * @return {@code false} if building the scenario failed, in which case the run is already over
   */
  private boolean resolveScenario() {
    try {
      robot = new UiRobot(testCase.screen(), syntheticInput);
      steps = testCase.scenario().steps();
      return true;
    } catch (Throwable t) {
      fail("<building scenario>", t);
      return false;
    }
  }

  private String currentStepName() {
    return steps == null || stepIndex >= steps.size() ? "<none>" : steps.get(stepIndex).name();
  }

  private void fail(String stepName, Throwable thrown) {
    failure = thrown;
    failedStep = stepName;
    Gdx.app.error("UiScenario", "step '" + stepName + "' failed: " + thrown, thrown);
    Gdx.app.exit();
  }
}
