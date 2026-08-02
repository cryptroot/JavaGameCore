package com.cryptroot.core.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.utils.Align;
import org.junit.jupiter.api.Test;

/**
 * Covers the alignment arithmetic that replaced the toolkit's hand-tuned baseline nudges. Pure
 * float maths — no font, no GL.
 */
class UiHelperTest {

  private static final float EPS = 1e-4f;

  // -------------------------------------------------------------------------
  // baselineIn
  // -------------------------------------------------------------------------

  @Test
  void baselineBottomSitsOnBoxBottom() {
    assertEquals(100f, UiHelper.baselineIn(100f, 40f, 15f, Align.bottom), EPS);
  }

  @Test
  void baselineTopPutsCapHeightAtBoxTop() {
    // Box spans 100..140; the cap top should touch 140, so the baseline is 140 - 15.
    assertEquals(125f, UiHelper.baselineIn(100f, 40f, 15f, Align.top), EPS);
  }

  @Test
  void baselineCenterMatchesTheFormulaWidgetsUsedInline() {
    // The pre-existing inline formula was `y + (h + capHeight) / 2`.
    assertEquals(127.5f, UiHelper.baselineIn(100f, 40f, 15f, Align.center), EPS);
  }

  /**
   * Unrecognised or combined masks centre rather than throwing — documented fail-soft behaviour.
   */
  @Test
  void baselineDefaultsToCenterForUnknownMask() {
    assertEquals(
        UiHelper.baselineIn(100f, 40f, 15f, Align.center),
        UiHelper.baselineIn(100f, 40f, 15f, Align.left),
        EPS);
  }

  @Test
  void baselineHandlesZeroCapHeight() {
    assertEquals(120f, UiHelper.baselineIn(100f, 40f, 0f, Align.center), EPS);
    assertEquals(140f, UiHelper.baselineIn(100f, 40f, 0f, Align.top), EPS);
  }

  @Test
  void baselineHandlesZeroHeightBox() {
    assertEquals(100f, UiHelper.baselineIn(100f, 0f, 0f, Align.center), EPS);
  }

  // -------------------------------------------------------------------------
  // alignIn
  // -------------------------------------------------------------------------

  @Test
  void alignLeftReturnsBoxLeft() {
    assertEquals(50f, UiHelper.alignIn(50f, 200f, 30f, Align.left), EPS);
  }

  @Test
  void alignRightEndsAtBoxRight() {
    assertEquals(220f, UiHelper.alignIn(50f, 200f, 30f, Align.right), EPS);
  }

  @Test
  void alignCenterCentresContent() {
    assertEquals(135f, UiHelper.alignIn(50f, 200f, 30f, Align.center), EPS);
  }

  /**
   * Content wider than its box is not clamped: centre and right alignment go negative relative to
   * the box so overflow stays visually symmetric instead of silently left-anchoring.
   */
  @Test
  void alignDoesNotClampOversizedContent() {
    assertEquals(-25f, UiHelper.alignIn(0f, 50f, 100f, Align.center), EPS);
    assertEquals(-50f, UiHelper.alignIn(0f, 50f, 100f, Align.right), EPS);
    assertEquals(0f, UiHelper.alignIn(0f, 50f, 100f, Align.left), EPS);
  }

  @Test
  void alignHandlesExactFit() {
    assertEquals(10f, UiHelper.alignIn(10f, 40f, 40f, Align.center), EPS);
    assertEquals(10f, UiHelper.alignIn(10f, 40f, 40f, Align.right), EPS);
  }

  // -------------------------------------------------------------------------
  // barHeight (pre-existing)
  // -------------------------------------------------------------------------

  @Test
  void barHeightRejectsNullFont() {
    org.junit.jupiter.api.Assertions.assertThrows(
        NullPointerException.class, () -> UiHelper.barHeight(null, 4f));
  }
}
