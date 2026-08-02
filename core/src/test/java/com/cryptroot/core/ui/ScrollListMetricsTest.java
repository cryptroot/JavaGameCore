package com.cryptroot.core.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link ScrollList#itemIndexAt}, the row-hit arithmetic.
 *
 * <p>Includes the divide-by-zero case: row height is derived during layout, so any pointer event
 * that arrives before the first layout pass previously divided by zero.
 */
class ScrollListMetricsTest {

  /** A list occupying y 100..300 with 20-unit rows and 5 items. */
  private static int rowAt(float worldY) {
    return ScrollList.itemIndexAt(worldY, 100f, 200f, 20f, 0f, 5);
  }

  @Test
  void topOfListIsRowZero() {
    assertEquals(0, rowAt(299f));
  }

  @Test
  void rowBoundariesMapToConsecutiveIndices() {
    assertEquals(0, rowAt(281f));
    assertEquals(1, rowAt(279f));
    assertEquals(2, rowAt(259f));
    assertEquals(4, rowAt(219f));
  }

  /**
   * Below the last populated row there is no item, even though the point is inside the list box.
   */
  @Test
  void areaBelowTheLastRowIsNotASelection() {
    assertEquals(ScrollList.NO_SELECTION, rowAt(150f));
  }

  @Test
  void pointAboveTheListIsNotASelection() {
    assertEquals(ScrollList.NO_SELECTION, rowAt(320f));
  }

  /** Scrolling shifts which item sits under a given point. */
  @Test
  void scrollOffsetShiftsRowMapping() {
    assertEquals(0, ScrollList.itemIndexAt(299f, 100f, 200f, 20f, 0f, 5));
    assertEquals(1, ScrollList.itemIndexAt(299f, 100f, 200f, 20f, 20f, 5));
    assertEquals(2, ScrollList.itemIndexAt(299f, 100f, 200f, 20f, 40f, 5));
  }

  /**
   * The regression case: item height is zero until the first layout pass, so an early pointer event
   * must report "no row" instead of dividing by zero.
   */
  @Test
  void zeroItemHeightYieldsNoSelectionInsteadOfDividingByZero() {
    assertEquals(ScrollList.NO_SELECTION, ScrollList.itemIndexAt(200f, 100f, 200f, 0f, 0f, 5));
    assertEquals(ScrollList.NO_SELECTION, ScrollList.itemIndexAt(200f, 100f, 200f, -5f, 0f, 5));
  }

  @Test
  void emptyListHasNoRows() {
    assertEquals(ScrollList.NO_SELECTION, ScrollList.itemIndexAt(200f, 100f, 200f, 20f, 0f, 0));
  }

  @Test
  void noSelectionIsNegativeOne() {
    assertEquals(-1, ScrollList.NO_SELECTION);
  }
}
