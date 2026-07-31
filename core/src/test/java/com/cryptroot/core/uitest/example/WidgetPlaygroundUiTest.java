package com.cryptroot.core.uitest.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.screen.BaseScreen;
import com.cryptroot.core.ui.ScrollList;
import com.cryptroot.core.uitest.CaptureAssertions;
import com.cryptroot.core.uitest.DisplayAvailability;
import com.cryptroot.core.uitest.UiScenario;
import com.cryptroot.core.uitest.UiTestApp;
import com.cryptroot.core.uitest.UiTestCase;
import com.cryptroot.core.uitest.UiTestConfig;
import com.cryptroot.core.uitest.WidgetQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The harness's own reference test: every input path it offers, driven against {@link
 * WidgetPlaygroundScreen} in a real window, with a screenshot at each stage.
 *
 * <p>Its job is to fail when the harness breaks, so a game's UI test failing means the game's UI
 * broke. Each stage therefore asserts an <em>observable</em> effect of the input (a counter, a
 * slider value, a selected row, field text) rather than trusting that the event was delivered.
 *
 * <p>One test method, one window: see {@link UiTestApp}'s note on GLFW and forks.
 */
class WidgetPlaygroundUiTest {

  @BeforeAll
  static void requireDisplay() {
    DisplayAvailability.assumeAvailable();
  }

  @Test
  void drivesEveryInputPath() {
    UiTestApp.run(
        UiTestConfig.defaults("core UI harness").withCaptureDir("target/uitest/core"),
        new PlaygroundCase());

    // Pixel-level checks run here rather than in a step: the images only have to prove something
    // was
    // drawn, and this keeps the assertion out of the render loop.
    CaptureAssertions.assertNotBlank("01-hover.png");
    CaptureAssertions.assertNotBlank("05-typed.png");
  }

  /** Builds the screen on the GL thread and scripts the interactions against it. */
  private static final class PlaygroundCase implements UiTestCase {

    private PlaygroundContext context;

    /**
     * Deliberately not called {@code screen}: inside the anonymous {@link Game} below, that simple
     * name resolves to {@code Game}'s own inherited {@code screen} field, so the assignment would
     * silently leave this one null.
     */
    private WidgetPlaygroundScreen screenUnderTest;

    /** Slider value before the drag, so the assertion needs no knowledge of the track geometry. */
    private final float[] sliderBefore = new float[1];

    /** Row index selected before scrolling, compared with the row at the same point afterwards. */
    private final int[] rowBeforeScroll = new int[1];

    /** The world point clicked in the list, reused so the comparison is like-for-like. */
    private final float[] listPoint = new float[2];

    @Override
    public ApplicationListener createGame() {
      return new Game() {
        @Override
        public void create() {
          context = new PlaygroundContext();
          screenUnderTest = new WidgetPlaygroundScreen(context);
          setScreen(screenUnderTest);
        }

        @Override
        public void dispose() {
          if (getScreen() != null) getScreen().dispose();
          if (context != null) context.dispose();
        }
      };
    }

    @Override
    public BaseScreen<?> screen() {
      return screenUnderTest;
    }

    @Override
    public UiScenario scenario() {
      return UiScenario.begin()
          // Let the initial resize lay the tree out before anything is resolved or clicked.
          .waitFrames(3)
          .hover(
              "hover the increment button",
              layer -> WidgetQuery.requireButton(layer, WidgetPlaygroundScreen.INCREMENT_LABEL))
          .capture("01-hover.png")

          // Click: the button arms on touchDown and emits onClick after its feedback delay, so the
          // effect is awaited rather than assumed.
          .click(
              "click increment",
              layer -> WidgetQuery.requireButton(layer, WidgetPlaygroundScreen.INCREMENT_LABEL))
          .waitUntil("counter reaches 1", () -> screenUnderTest.clicks() == 1)
          .check(
              "counter label redrawn",
              () -> assertEquals("Clicks: 1", screenUnderTest.counterLabelText()))
          .capture("02-clicked.png")
          .click(
              "click increment again",
              layer -> WidgetQuery.requireButton(layer, WidgetPlaygroundScreen.INCREMENT_LABEL))
          .waitUntil("counter reaches 2", () -> screenUnderTest.clicks() == 2)
          .check(
              "button found by query is the screen's own",
              () ->
                  assertEquals(
                      screenUnderTest.incrementButton(),
                      WidgetQuery.requireButton(
                          screenUnderTest.uiLayer(), WidgetPlaygroundScreen.INCREMENT_LABEL)))

          // Drag: a slider integrates pointer movement, so this proves the intermediate
          // touchDragged events land, not just the press and release.
          .run(
              "drag the slider right",
              robot -> {
                sliderBefore[0] = screenUnderTest.slider().getValue();
                Rectangle bounds = screenUnderTest.slider().getBounds();
                robot.drag(screenUnderTest.slider(), bounds.width * 0.4f, 0f);
              })
          .check(
              "slider value rose",
              () -> {
                float now = screenUnderTest.slider().getValue();
                assertTrue(
                    now > sliderBefore[0],
                    "slider did not move: " + sliderBefore[0] + " -> " + now);
                assertTrue(now <= 100f, "slider exceeded its maximum: " + now);
              })
          .capture("03-slider-dragged.png")

          // Scroll: clicking the same world point before and after must select different rows,
          // which
          // is only true if the scroll offset actually changed.
          .run(
              "select a row",
              robot -> {
                Rectangle bounds = screenUnderTest.list().getBounds();
                listPoint[0] = bounds.x + bounds.width / 2f;
                listPoint[1] = bounds.y + bounds.height / 2f;
                robot.clickWorld(listPoint[0], listPoint[1]);
              })
          .check(
              "a row was selected",
              () -> {
                rowBeforeScroll[0] = screenUnderTest.list().getSelectedIndex();
                assertTrue(
                    rowBeforeScroll[0] != ScrollList.NO_SELECTION,
                    "clicking the list selected nothing");
              })
          .run("scroll the list down", robot -> robot.scroll(screenUnderTest.list(), 5f))
          .run("re-select at the same point", robot -> robot.clickWorld(listPoint[0], listPoint[1]))
          .check(
              "the same point now hits a later row",
              () ->
                  assertTrue(
                      screenUnderTest.list().getSelectedIndex() > rowBeforeScroll[0],
                      "list did not scroll: row "
                          + rowBeforeScroll[0]
                          + " -> "
                          + screenUnderTest.list().getSelectedIndex()))
          .capture("04-list-scrolled.png")

          // Keyboard: focus follows a click exactly as at runtime, then characters and control keys
          // travel through the screen's own installed processor.
          .run("focus the text field", robot -> robot.click(screenUnderTest.field()))
          .run("type into the field", robot -> robot.type("ab"))
          .check("typed text arrived", () -> assertEquals("ab", screenUnderTest.field().getText()))
          .run("press backspace", robot -> robot.pressKey(Input.Keys.BACKSPACE))
          .check(
              "backspace deleted a character",
              () -> assertEquals("a", screenUnderTest.field().getText()))
          .capture("05-typed.png")
          .build();
    }
  }
}
