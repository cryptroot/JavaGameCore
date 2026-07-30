package com.cryptroot.core.ui.layout;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.List;

/**
 * Arranges children in a fixed number of equal-width columns, filling left to right and top to
 * bottom.
 *
 * <p>Column width is the content width divided evenly (minus {@linkplain #spacing(float) spacing}),
 * so columns line up regardless of content. Row height is the tallest natural height in that row,
 * so a row never clips its own contents. A final partial row is left ragged rather than stretched.
 *
 * <pre>{@code
 * GridStack keypad = new GridStack(3).spacing(4f);
 * for (int i = 1; i <= 9; i++) keypad.add(new Button(skin, String.valueOf(i)));
 * }</pre>
 */
public final class GridStack extends LayoutContainer<GridStack> {

  private final int columns;

  /**
   * @param columns number of columns; must be at least 1
   */
  public GridStack(int columns) {
    if (columns < 1) {
      throw new IllegalArgumentException("columns must be at least 1, got " + columns);
    }
    this.columns = columns;
  }

  /** Returns the column count this grid was created with. */
  public int columns() {
    return columns;
  }

  /** Number of rows needed for the current child count, including a ragged final row. */
  public int rowCount() {
    return rowsFor(childCount(), columns);
  }

  /** Rows required to hold {@code childCount} children in {@code columns} columns. */
  static int rowsFor(int childCount, int columns) {
    return (childCount + columns - 1) / columns;
  }

  /**
   * Natural size: {@code columns} times the widest child, by the summed tallest-per-row heights,
   * plus spacing and padding.
   */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    List<LayoutElement> kids = managed();
    int n = kids.size();
    Insets pad = padding();
    if (n == 0) {
      return out.set(pad.horizontal(), pad.vertical());
    }

    Vector2 tmp = measureScratch();
    float widest = 0f;
    float heightTotal = 0f;
    float rowTallest = 0f;
    for (int i = 0; i < n; i++) {
      kids.get(i).preferredSize(tmp);
      widest = Math.max(widest, tmp.x);
      rowTallest = Math.max(rowTallest, tmp.y);
      boolean endOfRow = (i % columns == columns - 1) || i == n - 1;
      if (endOfRow) {
        heightTotal += rowTallest;
        rowTallest = 0f;
      }
    }

    int rows = rowsFor(n, columns);
    return out.set(
        widest * columns + spacing() * (columns - 1) + pad.horizontal(),
        heightTotal + spacing() * (rows - 1) + pad.vertical());
  }

  @Override
  protected void arrange(Rectangle content) {
    List<LayoutElement> kids = managed();
    int n = kids.size();
    if (n == 0) return;

    float colWidth = Math.max(0f, (content.width - spacing() * (columns - 1)) / columns);
    Vector2 tmp = measureScratch();
    float rowTop = content.y + content.height;

    for (int start = 0; start < n; start += columns) {
      int end = Math.min(start + columns, n);

      // A row is as tall as its tallest member, so nothing in it is clipped.
      float rowHeight = 0f;
      for (int i = start; i < end; i++) {
        kids.get(i).preferredSize(tmp);
        rowHeight = Math.max(rowHeight, tmp.y);
      }

      for (int i = start; i < end; i++) {
        int column = i - start;
        float x = content.x + column * (colWidth + spacing());
        kids.get(i).setBounds(x, rowTop - rowHeight, colWidth, rowHeight);
      }
      rowTop -= rowHeight + spacing();
    }
  }
}
