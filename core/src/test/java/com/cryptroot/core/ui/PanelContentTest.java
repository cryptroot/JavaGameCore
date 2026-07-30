package com.cryptroot.core.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.ui.layout.Insets;
import org.junit.jupiter.api.Test;

/**
 * Covers the panel chrome arithmetic — content insets and tab-strip division — which previously
 * lived inline in methods that needed a {@link com.badlogic.gdx.graphics.Texture} and so could not
 * be tested at all.
 */
class PanelContentTest {

  private static final float EPS = 1e-4f;

  // -------------------------------------------------------------------------
  // Panel.contentBoundsFor
  // -------------------------------------------------------------------------

  @Test
  void plainPanelContentIsTheWholeFrame() {
    Rectangle out =
        Panel.contentBoundsFor(new Rectangle(10f, 20f, 300f, 200f), Insets.NONE, new Rectangle());

    assertEquals(10f, out.x, EPS);
    assertEquals(20f, out.y, EPS);
    assertEquals(300f, out.width, EPS);
    assertEquals(200f, out.height, EPS);
  }

  @Test
  void chromeInsetsShrinkContentOnEveryEdge() {
    Rectangle out =
        Panel.contentBoundsFor(
            new Rectangle(0f, 0f, 100f, 100f), new Insets(5f, 10f, 15f, 20f), new Rectangle());

    assertEquals(5f, out.x, EPS);
    assertEquals(10f, out.y, EPS);
    assertEquals(100f - 5f - 15f, out.width, EPS);
    assertEquals(100f - 10f - 20f, out.height, EPS);
  }

  /** The title bar comes off the top, matching CloseablePanel's declared chrome. */
  @Test
  void closeablePanelChromeReservesTitleBarAtTheTop() {
    Rectangle out =
        Panel.contentBoundsFor(
            new Rectangle(0f, 0f, 400f, 300f), CloseablePanel.CHROME, new Rectangle());

    // Content starts a padding above the bottom and stops below the title bar.
    assertEquals(CloseablePanel.CHROME.left(), out.x, EPS);
    assertEquals(CloseablePanel.CHROME.bottom(), out.y, EPS);
    assertEquals(400f - CloseablePanel.CHROME.horizontal(), out.width, EPS);
    assertEquals(300f - CloseablePanel.CHROME.vertical(), out.height, EPS);
    // The top inset must exceed the bottom one, because it also contains the title bar.
    org.junit.jupiter.api.Assertions.assertTrue(
        CloseablePanel.CHROME.top() > CloseablePanel.CHROME.bottom());
  }

  /** An over-inset panel degrades to an empty content area rather than an inverted rectangle. */
  @Test
  void oversizedChromeClampsContentToZero() {
    Rectangle out =
        Panel.contentBoundsFor(new Rectangle(0f, 0f, 10f, 10f), Insets.all(50f), new Rectangle());

    assertEquals(0f, out.width, EPS);
    assertEquals(0f, out.height, EPS);
  }

  @Test
  void contentBoundsRejectsNulls() {
    assertThrows(
        NullPointerException.class,
        () -> Panel.contentBoundsFor(null, Insets.NONE, new Rectangle()));
    assertThrows(
        NullPointerException.class,
        () -> Panel.contentBoundsFor(new Rectangle(), null, new Rectangle()));
    assertThrows(
        NullPointerException.class,
        () -> Panel.contentBoundsFor(new Rectangle(), Insets.NONE, null));
  }

  // -------------------------------------------------------------------------
  // TabbedPanel.tabRectFor
  // -------------------------------------------------------------------------

  @Test
  void tabsDivideThePanelWidthEvenly() {
    Rectangle first =
        TabbedPanel.tabRectFor(0, 3, 0f, 0f, 320f, 200f, 36f, 10f, true, new Rectangle());
    Rectangle last =
        TabbedPanel.tabRectFor(2, 3, 0f, 0f, 320f, 200f, 36f, 10f, true, new Rectangle());

    float expectedWidth = (320f - 20f) / 3f;
    assertEquals(expectedWidth, first.width, EPS);
    assertEquals(expectedWidth, last.width, EPS);
    assertEquals(0f, first.x, EPS);
    assertEquals(2f * (expectedWidth + 10f), last.x, EPS);
    assertEquals(320f, last.x + last.width, EPS);
  }

  @Test
  void topTabsSitAtThePanelTopAndBottomTabsAtTheBottom() {
    Rectangle top =
        TabbedPanel.tabRectFor(0, 1, 0f, 100f, 200f, 300f, 36f, 0f, true, new Rectangle());
    Rectangle bottom =
        TabbedPanel.tabRectFor(0, 1, 0f, 100f, 200f, 300f, 36f, 0f, false, new Rectangle());

    assertEquals(100f + 300f - 36f, top.y, EPS);
    assertEquals(100f, bottom.y, EPS);
    assertEquals(36f, top.height, EPS);
  }

  @Test
  void singleTabSpansTheFullWidth() {
    Rectangle only =
        TabbedPanel.tabRectFor(0, 1, 0f, 0f, 250f, 100f, 30f, 8f, true, new Rectangle());

    assertEquals(250f, only.width, EPS);
  }

  @Test
  void tabRectValidatesIndexAndCount() {
    assertThrows(
        IllegalArgumentException.class,
        () -> TabbedPanel.tabRectFor(0, 0, 0f, 0f, 10f, 10f, 5f, 0f, true, new Rectangle()));
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> TabbedPanel.tabRectFor(3, 3, 0f, 0f, 10f, 10f, 5f, 0f, true, new Rectangle()));
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> TabbedPanel.tabRectFor(-1, 3, 0f, 0f, 10f, 10f, 5f, 0f, true, new Rectangle()));
  }

  /** More gap than width must not produce negative tab widths. */
  @Test
  void oversizedGapClampsTabWidthToZero() {
    Rectangle out =
        TabbedPanel.tabRectFor(0, 3, 0f, 0f, 10f, 100f, 30f, 100f, true, new Rectangle());

    assertEquals(0f, out.width, EPS);
  }
}
