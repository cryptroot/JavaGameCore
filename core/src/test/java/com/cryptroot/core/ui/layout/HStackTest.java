package com.cryptroot.core.ui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.utils.Align;
import org.junit.jupiter.api.Test;

class HStackTest {

  private static final float EPS = 1e-3f;

  private static void layoutIn(HStack stack, float width, float height) {
    stack.setBounds(0f, 0f, width, height);
    stack.layout();
  }

  @Test
  void stacksChildrenLeftToRightWithSpacingAndPadding() {
    FakeElement a = new FakeElement(30f, 10f);
    FakeElement b = new FakeElement(40f, 10f);
    HStack stack = new HStack().padding(Insets.all(20f)).spacing(10f);
    stack.add(a).add(b);

    layoutIn(stack, 500f, 300f);

    assertEquals(20f, a.assigned.x, EPS);
    assertEquals(30f, a.assigned.width, EPS);
    assertEquals(20f + 30f + 10f, b.assigned.x, EPS);
    assertEquals(40f, b.assigned.width, EPS);
  }

  /** The motel's three-column row: equal weights must give equal widths. */
  @Test
  void threeEqualWeightsGiveThreeEqualColumns() {
    FakeElement a = new FakeElement(100f, 10f, 1f);
    FakeElement b = new FakeElement(300f, 10f, 1f);
    FakeElement c = new FakeElement(700f, 10f, 1f);
    HStack stack = new HStack().spacing(16f);
    stack.add(a).add(b).add(c);

    layoutIn(stack, 1000f, 100f);

    float expected = (1000f - 32f) / 3f;
    assertEquals(expected, a.assigned.width, EPS);
    assertEquals(expected, b.assigned.width, EPS);
    assertEquals(expected, c.assigned.width, EPS);

    // And they tile without gaps or overlap beyond the declared spacing.
    assertEquals(a.assigned.x + a.assigned.width + 16f, b.assigned.x, EPS);
    assertEquals(b.assigned.x + b.assigned.width + 16f, c.assigned.x, EPS);
    assertEquals(1000f, c.assigned.x + c.assigned.width, EPS);
  }

  @Test
  void stretchCrossFillsFullContentHeight() {
    FakeElement a = new FakeElement(10f, 5f);
    HStack stack = new HStack().padding(Insets.all(5f)).stretchCross(true);
    stack.add(a);

    layoutIn(stack, 100f, 200f);

    assertEquals(5f, a.assigned.y, EPS);
    assertEquals(190f, a.assigned.height, EPS);
  }

  @Test
  void crossAlignTopPushesChildToContentTop() {
    FakeElement a = new FakeElement(10f, 40f);
    HStack stack = new HStack().align(Align.topLeft);
    stack.add(a);

    layoutIn(stack, 100f, 200f);

    assertEquals(160f, a.assigned.y, EPS);
  }

  @Test
  void crossAlignBottomPushesChildToContentBottom() {
    FakeElement a = new FakeElement(10f, 40f);
    HStack stack = new HStack().align(Align.bottomLeft);
    stack.add(a);

    layoutIn(stack, 100f, 200f);

    assertEquals(0f, a.assigned.y, EPS);
  }

  @Test
  void crossAlignCentreCentresChildVertically() {
    FakeElement a = new FakeElement(10f, 40f);
    HStack stack = new HStack().align(Align.center);
    stack.add(a);

    layoutIn(stack, 100f, 200f);

    assertEquals(80f, a.assigned.y, EPS);
  }

  @Test
  void mainAlignRightPushesBlockToTheRight() {
    FakeElement a = new FakeElement(50f, 10f);
    HStack stack = new HStack().align(Align.topRight);
    stack.add(a);

    layoutIn(stack, 200f, 100f);

    assertEquals(150f, a.assigned.x, EPS);
  }

  /** A flexible spacer is how a row is split into left- and right-aligned halves. */
  @Test
  void flexibleSpacerPushesLaterChildrenToTheEnd() {
    FakeElement left = new FakeElement(40f, 10f);
    FakeElement right = new FakeElement(60f, 10f);
    HStack stack = new HStack();
    stack.add(left).add(Spacer.flexible()).add(right);

    layoutIn(stack, 300f, 100f);

    assertEquals(0f, left.assigned.x, EPS);
    assertEquals(240f, right.assigned.x, EPS);
    assertEquals(300f, right.assigned.x + right.assigned.width, EPS);
  }

  @Test
  void fixedSpacerInsertsExactGap() {
    FakeElement a = new FakeElement(10f, 10f);
    FakeElement b = new FakeElement(10f, 10f);
    HStack stack = new HStack();
    stack.add(a).add(Spacer.of(25f)).add(b);

    layoutIn(stack, 300f, 100f);

    assertEquals(35f, b.assigned.x, EPS);
  }

  @Test
  void preferredSizeSumsWidthsAndMaxesHeights() {
    HStack stack = new HStack().padding(Insets.symmetric(10f, 4f)).spacing(6f);
    stack.add(new FakeElement(30f, 20f)).add(new FakeElement(50f, 40f));

    com.badlogic.gdx.math.Vector2 size = stack.preferredSize(new com.badlogic.gdx.math.Vector2());

    assertEquals(30f + 50f + 6f + 20f, size.x, EPS);
    assertEquals(40f + 8f, size.y, EPS);
  }
}
