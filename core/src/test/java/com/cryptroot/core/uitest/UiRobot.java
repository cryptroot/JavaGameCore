package com.cryptroot.core.uitest;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cryptroot.core.screen.BaseScreen;
import com.cryptroot.core.ui.BoundedWidget;
import com.cryptroot.core.ui.UiLayer;
import java.util.Objects;

/**
 * Drives a running screen the way a player would: move, hover, click, drag, scroll, type.
 *
 * <h3>How input is delivered</h3>
 *
 * Every action does two things, in this order:
 *
 * <ol>
 *   <li>writes the pointer position into {@link SyntheticInput}, so the frame's <em>polled</em>
 *       cursor (hover, occlusion, scroll origin) agrees with the action, and
 *   <li>dispatches the event to the {@link InputProcessor} the screen itself installed — read back
 *       from {@code Gdx.input} rather than reconstructed, so a screen that composes its keyboard
 *       handler into an {@code InputMultiplexer} is exercised exactly as it ships.
 * </ol>
 *
 * <p>Nothing here touches the operating system: no {@code java.awt.Robot}, no cursor warping, no
 * screen scraping. That is what makes the same test pass on Windows, X11, XWayland and native
 * Wayland — see {@link SyntheticInput}.
 *
 * <h3>Coordinates</h3>
 *
 * Prefer the widget-taking overloads. They aim at the centre of the widget's own {@linkplain
 * BoundedWidget#getBounds() hit rectangle}, so a test never hard-codes a pixel and never has to be
 * updated when a layout shifts. World coordinates are converted by {@link ScreenPoint}.
 *
 * <h3>Threading</h3>
 *
 * All methods must be called on the GL thread — in practice, from inside a {@link UiScenario} step,
 * which {@link ScenarioDriver} runs at the end of a frame.
 */
public final class UiRobot {

  /**
   * Intermediate {@code touchDragged} events sent by {@link #drag(BoundedWidget, float, float)}.
   */
  private static final int DEFAULT_DRAG_STEPS = 8;

  private final BaseScreen<?> screen;
  private final SyntheticInput input;
  private final Vector3 scratch = new Vector3();

  UiRobot(BaseScreen<?> screen, SyntheticInput input) {
    this.screen = Objects.requireNonNull(screen, "screen must not be null");
    this.input = Objects.requireNonNull(input, "input must not be null");
  }

  /** The layer being driven — the argument for every {@link WidgetQuery} lookup. */
  public UiLayer layer() {
    return screen.uiLayer();
  }

  /** The screen being driven. */
  public BaseScreen<?> screen() {
    return screen;
  }

  private Viewport viewport() {
    return screen.uiLayer().getViewport();
  }

  /**
   * The processor the screen installed in {@code show()}.
   *
   * @throws IllegalStateException if none is installed, which means {@code show()} has not run yet
   *     — a scenario that acts before the first frame
   */
  private InputProcessor processor() {
    InputProcessor processor = input.getInputProcessor();
    if (processor == null) {
      throw new IllegalStateException(
          "no InputProcessor installed: the screen's show() has not run yet — "
              + "start the scenario with waitFrames(1) or more");
    }
    return processor;
  }

  // -------------------------------------------------------------------------
  // Pointer
  // -------------------------------------------------------------------------

  /** Moves the pointer to a y-down screen pixel. */
  public void moveToScreen(int screenX, int screenY) {
    input.moveTo(screenX, screenY);
    processor().mouseMoved(screenX, screenY);
  }

  /** Moves the pointer to a world-space point. */
  public void moveTo(float worldX, float worldY) {
    ScreenPoint.toScreen(viewport(), Gdx.graphics.getHeight(), worldX, worldY, scratch);
    moveToScreen(Math.round(scratch.x), Math.round(scratch.y));
  }

  /**
   * Moves the pointer over the centre of {@code widget} without clicking, so the next rendered
   * frame shows its hover state.
   */
  public void hover(BoundedWidget widget) {
    Rectangle bounds = requireLaidOut(widget);
    moveTo(bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f);
  }

  /** Presses and releases the left button at the centre of {@code widget}. */
  public void click(BoundedWidget widget) {
    Rectangle bounds = requireLaidOut(widget);
    clickWorld(bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f);
  }

  /** Presses and releases the left button at a world-space point. */
  public void clickWorld(float worldX, float worldY) {
    ScreenPoint.toScreen(viewport(), Gdx.graphics.getHeight(), worldX, worldY, scratch);
    clickScreen(Math.round(scratch.x), Math.round(scratch.y));
  }

  /** Presses and releases the left button at a y-down screen pixel. */
  public void clickScreen(int screenX, int screenY) {
    InputProcessor processor = processor();
    input.moveTo(screenX, screenY);
    processor.mouseMoved(screenX, screenY);
    input.pressLeft();
    processor.touchDown(screenX, screenY, 0, Input.Buttons.LEFT);
    input.releaseLeft();
    processor.touchUp(screenX, screenY, 0, Input.Buttons.LEFT);
  }

  /**
   * Presses at the centre of {@code widget}, drags by a world-space offset in {@value
   * #DEFAULT_DRAG_STEPS} intermediate moves, then releases.
   *
   * <p>Stepped rather than teleported because drag-driven widgets integrate movement: a slider that
   * only ever sees start and end would still pass a test that a real drag would fail.
   */
  public void drag(BoundedWidget widget, float worldDx, float worldDy) {
    drag(widget, worldDx, worldDy, DEFAULT_DRAG_STEPS);
  }

  /**
   * As {@link #drag(BoundedWidget, float, float)} with an explicit number of intermediate moves.
   *
   * @throws IllegalArgumentException if {@code steps} is less than 1
   */
  public void drag(BoundedWidget widget, float worldDx, float worldDy, int steps) {
    if (steps < 1) {
      throw new IllegalArgumentException("steps must be at least 1, got " + steps);
    }
    Rectangle bounds = requireLaidOut(widget);
    float startX = bounds.x + bounds.width / 2f;
    float startY = bounds.y + bounds.height / 2f;

    InputProcessor processor = processor();
    int[] from = toScreenPixels(startX, startY);
    input.moveTo(from[0], from[1]);
    input.pressLeft();
    processor.touchDown(from[0], from[1], 0, Input.Buttons.LEFT);

    for (int i = 1; i <= steps; i++) {
      float t = (float) i / steps;
      int[] at = toScreenPixels(startX + worldDx * t, startY + worldDy * t);
      input.moveTo(at[0], at[1]);
      processor.touchDragged(at[0], at[1], 0);
    }

    int[] to = toScreenPixels(startX + worldDx, startY + worldDy);
    input.releaseLeft();
    processor.touchUp(to[0], to[1], 0, Input.Buttons.LEFT);
  }

  /**
   * Scrolls with the pointer over the centre of {@code widget}.
   *
   * <p>The pointer must be moved first and not merely passed along: {@code UiLayer}'s scroll
   * routing reads the <em>polled</em> cursor position, not the event's, because a scroll event
   * carries only an amount.
   *
   * @param amountY libGDX convention — positive scrolls down, negative scrolls up
   */
  public void scroll(BoundedWidget widget, float amountY) {
    hover(widget);
    processor().scrolled(0f, amountY);
  }

  // -------------------------------------------------------------------------
  // Keyboard
  // -------------------------------------------------------------------------

  /** Sends a key down/up pair, e.g. {@code Input.Keys.BACKSPACE}. */
  public void pressKey(int keycode) {
    InputProcessor processor = processor();
    processor.keyDown(keycode);
    processor.keyUp(keycode);
  }

  /**
   * Types printable characters into whichever widget holds focus.
   *
   * <p>Focus follows a click, exactly as at runtime — click the field first. Control characters are
   * rejected rather than silently dropped, because {@code UiLayer} routes those through {@code
   * keyDown} and {@link #pressKey(int)} is the honest way to send them.
   *
   * @throws IllegalArgumentException if {@code text} contains a control character
   */
  public void type(String text) {
    Objects.requireNonNull(text, "text must not be null");
    InputProcessor processor = processor();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c < 32 || c == 127) {
        throw new IllegalArgumentException(
            "type() takes printable characters only; use pressKey() for control keys, got 0x"
                + Integer.toHexString(c));
      }
      processor.keyTyped(c);
    }
  }

  // -------------------------------------------------------------------------
  // Internals
  // -------------------------------------------------------------------------

  private int[] toScreenPixels(float worldX, float worldY) {
    ScreenPoint.toScreen(viewport(), Gdx.graphics.getHeight(), worldX, worldY, scratch);
    return new int[] {Math.round(scratch.x), Math.round(scratch.y)};
  }

  /**
   * A widget's hit rectangle, refusing to aim at one that has never been laid out.
   *
   * <p>An empty rectangle means "clicked (0,0)", which lands on whatever happens to be in the
   * bottom-left corner — a false pass or a baffling failure, either way not the message the author
   * needs.
   *
   * @throws AssertionError if the widget's bounds are empty
   */
  private Rectangle requireLaidOut(BoundedWidget widget) {
    Objects.requireNonNull(widget, "widget must not be null");
    Rectangle bounds = widget.getBounds();
    if (bounds.width <= 0f || bounds.height <= 0f) {
      throw new AssertionError(
          widget.getClass().getSimpleName()
              + " has empty bounds "
              + bounds
              + " — it has not been laid out, so there is nothing to aim at");
    }
    return bounds;
  }
}
