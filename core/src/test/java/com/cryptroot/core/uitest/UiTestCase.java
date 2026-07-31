package com.cryptroot.core.uitest;

import com.badlogic.gdx.ApplicationListener;
import com.cryptroot.core.screen.BaseScreen;

/**
 * What a test supplies to {@link UiTestApp}: the game to run, the screen to drive, and the script
 * to run against it.
 *
 * <p>Three methods rather than three constructor arguments because none of them can be evaluated
 * before there is a GL context — a {@link com.cryptroot.core.GameContext GameContext} allocates
 * textures and rasterises fonts, so it cannot exist on the test thread. {@link #createGame()} is
 * called inside {@code create()}, and the other two immediately after, by which time the
 * implementation can hand back the objects it built.
 *
 * <p>The usual implementation is an inner class holding the context and screen as fields:
 *
 * <pre>{@code
 * private static final class Case implements UiTestCase {
 *   private MyGameContext context;
 *   private MyScreen screenUnderTest;   // NOT "screen" — see below
 *
 *   public ApplicationListener createGame() {
 *     return new Game() {
 *       public void create() {
 *         context = new MyGameContext(scriptedRandom);
 *         screenUnderTest = new MyScreen(context);
 *         setScreen(screenUnderTest);
 *       }
 *     };
 *   }
 *
 *   public BaseScreen<?> screen() { return screenUnderTest; }
 *
 *   public UiScenario scenario() { return UiScenario.begin()...build(); }
 * }
 * }</pre>
 *
 * <p>Do not name that field {@code screen}. Inside the anonymous {@link com.badlogic.gdx.Game
 * Game}, the simple name {@code screen} resolves to {@code Game}'s own inherited {@code screen}
 * field, so the assignment compiles, runs, and leaves the outer field null — which surfaces much
 * later as "screen must not be null" while building the scenario.
 */
public interface UiTestCase {

  /**
   * Creates the game to run. Called once, on the GL thread, before {@code create()} is forwarded to
   * it — so the returned listener's own {@code create()} has not run yet.
   */
  ApplicationListener createGame();

  /**
   * The screen to drive. Called after the game's {@code create()} has run, so a field assigned
   * there is safe to return.
   */
  BaseScreen<?> screen();

  /**
   * The script to run. Called once, after {@link #screen()}, so the steps may close over the
   * context and screen built in {@link #createGame()}.
   */
  UiScenario scenario();
}
