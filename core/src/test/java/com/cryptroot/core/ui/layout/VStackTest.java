package com.cryptroot.core.ui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.Align;
import org.junit.jupiter.api.Test;

class VStackTest {

  private static final float EPS = 1e-3f;

  /** Lays out {@code stack} in a frame at the origin. */
  private static void layoutIn(VStack stack, float width, float height) {
    stack.setBounds(0f, 0f, width, height);
    stack.layout();
  }

  // -------------------------------------------------------------------------
  // Basic stacking
  // -------------------------------------------------------------------------

  @Test
  void stacksChildrenTopDownWithSpacingAndPadding() {
    FakeElement a = new FakeElement(100f, 30f);
    FakeElement b = new FakeElement(100f, 40f);
    FakeElement c = new FakeElement(100f, 20f);
    VStack stack = new VStack().padding(Insets.all(20f)).spacing(10f);
    stack.add(a).add(b).add(c);

    layoutIn(stack, 500f, 300f);

    // Content area is y 20..280. First child hangs from the top.
    assertEquals(280f - 30f, a.assigned.y, EPS);
    assertEquals(30f, a.assigned.height, EPS);
    // Then a 10-unit gap.
    assertEquals(250f - 10f - 40f, b.assigned.y, EPS);
    assertEquals(40f, b.assigned.height, EPS);
    assertEquals(200f - 10f - 20f, c.assigned.y, EPS);
    assertEquals(20f, c.assigned.height, EPS);

    // Left-aligned by default at the content's left edge.
    assertEquals(20f, a.assigned.x, EPS);
  }

  @Test
  void singleChildIsPlacedAtContentTop() {
    FakeElement only = new FakeElement(50f, 25f);
    VStack stack = new VStack();
    stack.add(only);

    layoutIn(stack, 200f, 100f);

    assertEquals(75f, only.assigned.y, EPS);
    assertEquals(25f, only.assigned.height, EPS);
  }

  @Test
  void emptyStackLaysOutWithoutError() {
    VStack stack = new VStack().padding(Insets.all(10f)).spacing(5f);
    layoutIn(stack, 100f, 100f);
    assertEquals(0, stack.childCount());
  }

  // -------------------------------------------------------------------------
  // Grow weights
  // -------------------------------------------------------------------------

  /**
   * The behaviour that makes three panels with equal weights come out as equal columns: a weighted
   * child is sized from its weight alone, so differing natural sizes cannot skew the result.
   */
  @Test
  void equalWeightsProduceEqualSizesRegardlessOfNaturalSize() {
    FakeElement small = new FakeElement(10f, 10f, 1f);
    FakeElement huge = new FakeElement(10f, 500f, 1f);
    VStack stack = new VStack();
    stack.add(small).add(huge);

    layoutIn(stack, 100f, 300f);

    assertEquals(150f, small.assigned.height, EPS);
    assertEquals(150f, huge.assigned.height, EPS);
  }

  @Test
  void weightsSplitProportionally() {
    FakeElement one = new FakeElement(10f, 0f, 1f);
    FakeElement two = new FakeElement(10f, 0f, 2f);
    VStack stack = new VStack();
    stack.add(one).add(two);

    layoutIn(stack, 100f, 300f);

    assertEquals(100f, one.assigned.height, EPS);
    assertEquals(200f, two.assigned.height, EPS);
  }

  /** Unweighted children keep their natural size; only the remainder is shared out. */
  @Test
  void unweightedChildrenKeepNaturalSizeAndWeightedTakeTheRest() {
    FakeElement fixed = new FakeElement(10f, 80f);
    FakeElement flexible = new FakeElement(10f, 5f, 1f);
    VStack stack = new VStack().spacing(20f);
    stack.add(fixed).add(flexible);

    layoutIn(stack, 100f, 300f);

    assertEquals(80f, fixed.assigned.height, EPS);
    // 300 available - 80 fixed - 20 spacing = 200
    assertEquals(200f, flexible.assigned.height, EPS);
  }

  /** No rounding seam: the distributed sizes plus spacing exactly fill the container. */
  @Test
  void weightedChildrenExactlyFillContainer() {
    FakeElement a = new FakeElement(0f, 0f, 1f);
    FakeElement b = new FakeElement(0f, 0f, 1f);
    FakeElement c = new FakeElement(0f, 0f, 1f);
    VStack stack = new VStack().spacing(7f);
    stack.add(a).add(b).add(c);

    layoutIn(stack, 100f, 1000f);

    float total = a.assigned.height + b.assigned.height + c.assigned.height + 7f * 2f;
    assertEquals(1000f, total, EPS);
  }

  // -------------------------------------------------------------------------
  // Cross axis
  // -------------------------------------------------------------------------

  @Test
  void stretchCrossFillsFullContentWidth() {
    FakeElement a = new FakeElement(10f, 20f);
    VStack stack = new VStack().padding(Insets.all(5f)).stretchCross(true);
    stack.add(a);

    layoutIn(stack, 200f, 100f);

    assertEquals(5f, a.assigned.x, EPS);
    assertEquals(190f, a.assigned.width, EPS);
  }

  @Test
  void crossAlignRightPushesChildToContentRight() {
    FakeElement a = new FakeElement(40f, 20f);
    VStack stack = new VStack().align(Align.topRight);
    stack.add(a);

    layoutIn(stack, 200f, 100f);

    assertEquals(160f, a.assigned.x, EPS);
    assertEquals(40f, a.assigned.width, EPS);
  }

  @Test
  void crossAlignCentreCentresChildHorizontally() {
    FakeElement a = new FakeElement(40f, 20f);
    VStack stack = new VStack().align(Align.center);
    stack.add(a);

    layoutIn(stack, 200f, 100f);

    assertEquals(80f, a.assigned.x, EPS);
  }

  /**
   * A child wider than the container is capped at the content width rather than overflowing
   * sideways.
   */
  @Test
  void crossSizeIsCappedAtContentWidth() {
    FakeElement wide = new FakeElement(500f, 20f);
    VStack stack = new VStack();
    stack.add(wide);

    layoutIn(stack, 200f, 100f);

    assertEquals(200f, wide.assigned.width, EPS);
  }

  // -------------------------------------------------------------------------
  // Main-axis alignment of the whole block
  // -------------------------------------------------------------------------

  @Test
  void mainAlignBottomPushesBlockDown() {
    FakeElement a = new FakeElement(10f, 20f);
    VStack stack = new VStack().align(Align.bottomLeft);
    stack.add(a);

    layoutIn(stack, 100f, 100f);

    assertEquals(0f, a.assigned.y, EPS);
  }

  @Test
  void mainAlignCentreCentresBlockVertically() {
    FakeElement a = new FakeElement(10f, 20f);
    VStack stack = new VStack().align(Align.center);
    stack.add(a);

    layoutIn(stack, 100f, 100f);

    // 80 slack, half above: block top at 60, so child bottom at 40.
    assertEquals(40f, a.assigned.y, EPS);
  }

  // -------------------------------------------------------------------------
  // Overflow and degenerate frames
  // -------------------------------------------------------------------------

  /** Children larger than the container overflow downward rather than being silently squashed. */
  @Test
  void overflowingChildrenKeepNaturalSizeAndRunPastTheBottom() {
    FakeElement a = new FakeElement(10f, 80f);
    FakeElement b = new FakeElement(10f, 80f);
    VStack stack = new VStack();
    stack.add(a).add(b);

    layoutIn(stack, 100f, 100f);

    assertEquals(80f, a.assigned.height, EPS);
    assertEquals(80f, b.assigned.height, EPS);
    assertTrue(b.assigned.y < 0f, "second child should overflow below the container");
  }

  /** Over-padding clamps the content area to zero instead of inverting the rectangle. */
  @Test
  void paddingLargerThanFrameYieldsZeroSizedContent() {
    FakeElement a = new FakeElement(10f, 10f, 1f);
    VStack stack = new VStack().padding(Insets.all(100f));
    stack.add(a);

    layoutIn(stack, 50f, 50f);

    assertEquals(0f, a.assigned.width, EPS);
    assertEquals(0f, a.assigned.height, EPS);
  }

  // -------------------------------------------------------------------------
  // Measure
  // -------------------------------------------------------------------------

  @Test
  void preferredSizeSumsMainAxisAndMaxesCrossAxisPlusPadding() {
    VStack stack = new VStack().padding(Insets.all(10f)).spacing(5f);
    stack.add(new FakeElement(30f, 20f)).add(new FakeElement(50f, 40f));

    com.badlogic.gdx.math.Vector2 size = stack.preferredSize(new com.badlogic.gdx.math.Vector2());

    assertEquals(50f + 20f, size.x, EPS); // widest child + horizontal padding
    assertEquals(20f + 40f + 5f + 20f, size.y, EPS); // heights + spacing + vertical padding
  }

  // -------------------------------------------------------------------------
  // Rebuild
  // -------------------------------------------------------------------------

  /** The refresh path: clear the rows, add new ones, lay out again. */
  @Test
  void removeAllThenReAddRearranges() {
    VStack stack = new VStack();
    stack.add(new FakeElement(10f, 50f));
    layoutIn(stack, 100f, 100f);

    stack.removeAll();
    assertEquals(0, stack.childCount());

    FakeElement replacement = new FakeElement(10f, 25f);
    stack.add(replacement);
    layoutIn(stack, 100f, 100f);

    assertEquals(1, stack.childCount());
    assertEquals(75f, replacement.assigned.y, EPS);
  }

  @Test
  void layingOutTwiceIsIdempotent() {
    FakeElement a = new FakeElement(10f, 30f);
    VStack stack = new VStack().padding(Insets.all(5f));
    stack.add(a);

    layoutIn(stack, 100f, 100f);
    float firstY = a.assigned.y;
    layoutIn(stack, 100f, 100f);

    assertEquals(firstY, a.assigned.y, EPS);
  }
}
