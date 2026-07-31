package com.cryptroot.core.uitest;

/**
 * One entry in a {@link UiScenario}, ticked once per rendered frame until it reports completion.
 *
 * <p>A single functional shape covers every kind of step — act, wait, assert, capture — so {@link
 * ScenarioDriver} needs no per-kind branching, and a test can add a bespoke step without touching
 * the driver. Build them with the {@link UiScenario} builder rather than implementing this
 * directly.
 *
 * <p>A step signals failure by throwing: {@link ScenarioDriver} records the throwable together with
 * this step's {@link #name()} and ends the run, and the test thread rethrows it. That is how plain
 * JUnit assertions inside a step reach the report.
 */
public interface Step {

  /**
   * Short description used in progress logging and failure messages, e.g. {@code click Confirm}.
   */
  String name();

  /**
   * Advances this step by one frame.
   *
   * @param robot the input driver, on the GL thread, at the end of a fully rendered frame
   * @param frame how many times this method has already been called for this step, starting at 0
   * @return {@code true} when the step is finished and the scenario should move on
   */
  boolean tick(UiRobot robot, int frame);
}
