package com.cryptroot.core.ui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Directly exercises {@link AxisStack#resolveMainSizes}, the rule that decides how much main-axis
 * space each child gets. Kept separate from the stack tests because this is the piece most likely
 * to be changed later, and the intended semantics are easy to get subtly wrong.
 */
class AxisStackSizingTest {

  private static final float EPS = 1e-3f;

  @Test
  void noWeightsMeansEveryChildKeepsItsNaturalSize() {
    float[] natural = {10f, 20f, 30f};
    float[] weights = {0f, 0f, 0f};
    float[] out = new float[3];

    AxisStack.resolveMainSizes(natural, weights, 3, 1000f, 0f, out);

    assertEquals(10f, out[0], EPS);
    assertEquals(20f, out[1], EPS);
    assertEquals(30f, out[2], EPS);
  }

  /** The key rule: a weighted child ignores its natural size entirely. */
  @Test
  void weightedChildIsSizedFromWeightNotNaturalSize() {
    float[] natural = {999f, 1f};
    float[] weights = {1f, 1f};
    float[] out = new float[2];

    AxisStack.resolveMainSizes(natural, weights, 2, 200f, 0f, out);

    assertEquals(100f, out[0], EPS);
    assertEquals(100f, out[1], EPS);
  }

  @Test
  void spacingIsRemovedFromThePoolBeforeSharing() {
    float[] natural = {0f, 0f};
    float[] weights = {1f, 1f};
    float[] out = new float[2];

    AxisStack.resolveMainSizes(natural, weights, 2, 220f, 20f, out);

    assertEquals(100f, out[0], EPS);
    assertEquals(100f, out[1], EPS);
  }

  @Test
  void unweightedNaturalSizesAreReservedFirst() {
    float[] natural = {50f, 0f};
    float[] weights = {0f, 1f};
    float[] out = new float[2];

    AxisStack.resolveMainSizes(natural, weights, 2, 200f, 0f, out);

    assertEquals(50f, out[0], EPS);
    assertEquals(150f, out[1], EPS);
  }

  @Test
  void unevenWeightsSplitProportionally() {
    float[] natural = {0f, 0f, 0f};
    float[] weights = {1f, 2f, 3f};
    float[] out = new float[3];

    AxisStack.resolveMainSizes(natural, weights, 3, 600f, 0f, out);

    assertEquals(100f, out[0], EPS);
    assertEquals(200f, out[1], EPS);
    assertEquals(300f, out[2], EPS);
  }

  /**
   * Weights that do not divide evenly must still fill the container exactly — the last weighted
   * child absorbs the remainder rather than every child being rounded independently.
   */
  @Test
  void awkwardWeightsStillFillTheContainerExactly() {
    float[] natural = {0f, 0f, 0f};
    float[] weights = {1f, 1f, 1f};
    float[] out = new float[3];

    AxisStack.resolveMainSizes(natural, weights, 3, 100f, 0f, out);

    assertEquals(100f, out[0] + out[1] + out[2], EPS);
  }

  /** Over-subscribed containers clamp the pool at zero rather than handing out negative sizes. */
  @Test
  void poolClampsAtZeroWhenUnweightedContentAlreadyOverflows() {
    float[] natural = {500f, 0f};
    float[] weights = {0f, 1f};
    float[] out = new float[2];

    AxisStack.resolveMainSizes(natural, weights, 2, 100f, 0f, out);

    assertEquals(500f, out[0], EPS); // natural size preserved, overflow allowed
    assertEquals(0f, out[1], EPS); // nothing left to give
  }

  @Test
  void singleWeightedChildTakesEverything() {
    float[] natural = {5f};
    float[] weights = {1f};
    float[] out = new float[1];

    AxisStack.resolveMainSizes(natural, weights, 1, 250f, 12f, out);

    // One child means no spacing is consumed.
    assertEquals(250f, out[0], EPS);
  }

  @Test
  void zeroAvailableSpaceGivesWeightedChildrenNothing() {
    float[] natural = {10f};
    float[] weights = {1f};
    float[] out = new float[1];

    AxisStack.resolveMainSizes(natural, weights, 1, 0f, 0f, out);

    assertEquals(0f, out[0], EPS);
  }
}
