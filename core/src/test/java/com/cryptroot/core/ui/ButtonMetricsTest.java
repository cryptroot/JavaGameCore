package com.cryptroot.core.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.ui.layout.Insets;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the button sizing rule.
 *
 * <p>This is the arithmetic behind the original bug report: rows of buttons were spaced by a
 * hand-chosen pitch that was smaller than the height the buttons actually derive from their font
 * and padding, so every row overlapped the one above it. Pinning the rule down here means a change
 * to padding or measurement can no longer silently reintroduce that.
 */
class ButtonMetricsTest {

  private static final float EPS = 1e-4f;

  @Test
  void preferredSizeIsTextPlusPaddingOnBothAxes() {
    Insets padding = Insets.symmetric(12f, 6f);
    Vector2 size = Button.preferredSizeFor(100f, 15f, padding, new Vector2());

    assertEquals(100f + 24f, size.x, EPS);
    assertEquals(15f + 12f, size.y, EPS);
  }

  /**
   * The concrete numbers from the bug: a body-font row button is materially taller than the 40-unit
   * pitch the screen was using, which is why rows collided.
   */
  @Test
  void bodyFontRowIsTallerThanTheOldFortyUnitPitch() {
    // Cap height of the 34px body face is ~24 units; the old padding was 10 bottom + 20 top.
    Vector2 size =
        Button.preferredSizeFor(200f, 24f, new Insets(20f, 10f, 20f, 20f), new Vector2());

    assertTrue(
        size.y > 40f,
        "a body-font button measures " + size.y + " units tall, so a 40-unit row pitch overlaps");
  }

  @Test
  void zeroLengthTextStillLeavesRoomForPadding() {
    Vector2 size = Button.preferredSizeFor(0f, 15f, Insets.all(8f), new Vector2());

    assertEquals(16f, size.x, EPS);
    assertEquals(31f, size.y, EPS);
  }

  @Test
  void noPaddingGivesExactlyTheTextBox() {
    Vector2 size = Button.preferredSizeFor(80f, 20f, Insets.NONE, new Vector2());

    assertEquals(80f, size.x, EPS);
    assertEquals(20f, size.y, EPS);
  }

  /** Stacking N buttons of the measured height plus a gap must never overlap. */
  @Test
  void stackedRowsDoNotOverlapWhenPitchComesFromMeasurement() {
    Vector2 size = Button.preferredSizeFor(120f, 24f, Insets.symmetric(12f, 6f), new Vector2());
    float gap = 4f;
    float pitch = size.y + gap;

    float firstBottom = 500f - size.y;
    float secondTop = 500f - pitch;

    assertTrue(
        secondTop <= firstBottom, "the second row's top must not rise above the first's bottom");
    assertEquals(gap, firstBottom - secondTop, EPS);
  }

  @Test
  void nullArgumentsAreRejected() {
    assertThrows(
        NullPointerException.class, () -> Button.preferredSizeFor(1f, 1f, null, new Vector2()));
    assertThrows(
        NullPointerException.class, () -> Button.preferredSizeFor(1f, 1f, Insets.NONE, null));
  }
}
