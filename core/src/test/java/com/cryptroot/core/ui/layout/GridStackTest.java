package com.cryptroot.core.ui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GridStackTest {

  private static final float EPS = 1e-3f;

  private static void layoutIn(GridStack grid, float width, float height) {
    grid.setBounds(0f, 0f, width, height);
    grid.layout();
  }

  @Test
  void columnCountMustBePositive() {
    assertThrows(IllegalArgumentException.class, () -> new GridStack(0));
    assertThrows(IllegalArgumentException.class, () -> new GridStack(-2));
  }

  @Test
  void columnsShareContentWidthEvenly() {
    FakeElement a = new FakeElement(10f, 20f);
    FakeElement b = new FakeElement(10f, 20f);
    FakeElement c = new FakeElement(10f, 20f);
    GridStack grid = new GridStack(3).spacing(10f);
    grid.add(a).add(b).add(c);

    layoutIn(grid, 320f, 200f);

    float expected = (320f - 20f) / 3f;
    assertEquals(expected, a.assigned.width, EPS);
    assertEquals(expected, b.assigned.width, EPS);
    assertEquals(expected, c.assigned.width, EPS);
    assertEquals(0f, a.assigned.x, EPS);
    assertEquals(expected + 10f, b.assigned.x, EPS);
  }

  /** A row is as tall as its tallest member so nothing in it is clipped. */
  @Test
  void rowHeightIsTheTallestChildInThatRow() {
    FakeElement short1 = new FakeElement(10f, 20f);
    FakeElement tall = new FakeElement(10f, 60f);
    GridStack grid = new GridStack(2);
    grid.add(short1).add(tall);

    layoutIn(grid, 200f, 200f);

    assertEquals(60f, short1.assigned.height, EPS);
    assertEquals(60f, tall.assigned.height, EPS);
    assertEquals(140f, short1.assigned.y, EPS);
  }

  @Test
  void wrapsToTheNextRowAfterEachFullRow() {
    FakeElement a = new FakeElement(10f, 30f);
    FakeElement b = new FakeElement(10f, 30f);
    FakeElement c = new FakeElement(10f, 30f);
    GridStack grid = new GridStack(2).spacing(10f);
    grid.add(a).add(b).add(c);

    layoutIn(grid, 200f, 200f);

    // First row hangs from the top; second row sits a spacing below it.
    assertEquals(170f, a.assigned.y, EPS);
    assertEquals(170f, b.assigned.y, EPS);
    assertEquals(170f - 30f - 10f, c.assigned.y, EPS);
    assertEquals(0f, c.assigned.x, EPS); // wrapped back to the first column
  }

  /** A partial final row keeps its natural cell widths rather than stretching to fill. */
  @Test
  void raggedFinalRowIsNotStretched() {
    GridStack grid = new GridStack(3);
    FakeElement a = new FakeElement(10f, 10f);
    FakeElement b = new FakeElement(10f, 10f);
    FakeElement c = new FakeElement(10f, 10f);
    FakeElement d = new FakeElement(10f, 10f);
    grid.add(a).add(b).add(c).add(d);

    layoutIn(grid, 300f, 300f);

    assertEquals(100f, a.assigned.width, EPS);
    assertEquals(100f, d.assigned.width, EPS);
    assertEquals(0f, d.assigned.x, EPS);
  }

  @Test
  void rowCountAccountsForPartialRows() {
    GridStack grid = new GridStack(3);
    assertEquals(0, grid.rowCount());
    grid.add(new FakeElement(1f, 1f));
    assertEquals(1, grid.rowCount());
    grid.add(new FakeElement(1f, 1f)).add(new FakeElement(1f, 1f));
    assertEquals(1, grid.rowCount());
    grid.add(new FakeElement(1f, 1f));
    assertEquals(2, grid.rowCount());
  }

  @Test
  void emptyGridLaysOutWithoutError() {
    GridStack grid = new GridStack(4);
    layoutIn(grid, 100f, 100f);
    assertEquals(0, grid.childCount());
  }

  @Test
  void preferredSizeUsesWidestChildTimesColumns() {
    GridStack grid = new GridStack(2).spacing(10f).padding(Insets.all(5f));
    grid.add(new FakeElement(40f, 20f)).add(new FakeElement(60f, 30f));

    com.badlogic.gdx.math.Vector2 size = grid.preferredSize(new com.badlogic.gdx.math.Vector2());

    assertEquals(60f * 2f + 10f + 10f, size.x, EPS);
    assertEquals(30f + 10f, size.y, EPS); // one row, tallest child, plus padding
  }
}
