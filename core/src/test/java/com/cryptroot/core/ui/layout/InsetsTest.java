package com.cryptroot.core.ui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InsetsTest {

  private static final float EPS = 1e-5f;

  @Test
  void allSetsEveryEdge() {
    Insets insets = Insets.all(7f);
    assertEquals(7f, insets.left(), EPS);
    assertEquals(7f, insets.bottom(), EPS);
    assertEquals(7f, insets.right(), EPS);
    assertEquals(7f, insets.top(), EPS);
  }

  @Test
  void symmetricSplitsHorizontalAndVertical() {
    Insets insets = Insets.symmetric(12f, 6f);
    assertEquals(12f, insets.left(), EPS);
    assertEquals(12f, insets.right(), EPS);
    assertEquals(6f, insets.bottom(), EPS);
    assertEquals(6f, insets.top(), EPS);
  }

  @Test
  void horizontalAndVerticalSumOppositeEdges() {
    Insets insets = new Insets(1f, 2f, 3f, 4f);
    assertEquals(4f, insets.horizontal(), EPS); // left + right
    assertEquals(6f, insets.vertical(), EPS); // bottom + top
  }

  @Test
  void noneIsZeroOnEveryEdge() {
    assertEquals(0f, Insets.NONE.horizontal(), EPS);
    assertEquals(0f, Insets.NONE.vertical(), EPS);
  }

  /**
   * A negative inset would expand a container's content area beyond its own frame, which is a bug
   * at the call site rather than an intent worth honouring.
   */
  @Test
  void negativeEdgeIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new Insets(-1f, 0f, 0f, 0f));
    assertThrows(IllegalArgumentException.class, () -> new Insets(0f, -1f, 0f, 0f));
    assertThrows(IllegalArgumentException.class, () -> new Insets(0f, 0f, -1f, 0f));
    assertThrows(IllegalArgumentException.class, () -> new Insets(0f, 0f, 0f, -1f));
    assertThrows(IllegalArgumentException.class, () -> Insets.all(-3f));
  }

  @Test
  void zeroIsAccepted() {
    Insets insets = Insets.all(0f);
    assertEquals(0f, insets.horizontal(), EPS);
  }
}
