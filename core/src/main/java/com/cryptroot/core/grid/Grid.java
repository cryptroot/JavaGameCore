package com.cryptroot.core.grid;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import java.util.Objects;
import java.util.Optional;

/**
 * A rectangular grid laid out in world space, converting between integer cell coordinates and
 * continuous world positions.
 *
 * <p>This is standalone geometry — the framework equivalent of a Unity {@code GridManager}'s
 * coordinate math. It is deliberately independent of any tilemap: a game supplies the origin and
 * cell size directly (a Tiled map can still derive one via {@code tiled}'s {@code TiledGrids}).
 * Cell {@code (0,0)} is the bottom-left cell; column increases with +X, row with +Y, matching the
 * bottom-left/y-up world convention used throughout {@code core}.
 *
 * <p>Cells may be non-square: separate {@link #cellWidth()} and {@link #cellHeight()} are stored,
 * and the single-{@code cellSize} constructor simply sets them equal. Any game-specific notion of
 * which cells are placeable, walkable, a spawn, etc. belongs in the game — this class only knows
 * geometry and bounds.
 */
public final class Grid {

  private final float originX;
  private final float originY;
  private final float cellWidth;
  private final float cellHeight;
  private final int columns;
  private final int rows;

  /** Square-cell grid: {@code cellWidth == cellHeight == cellSize}. */
  public Grid(float originX, float originY, float cellSize, int columns, int rows) {
    this(originX, originY, cellSize, cellSize, columns, rows);
  }

  /**
   * @param originX world X of the bottom-left corner of cell (0,0)
   * @param originY world Y of the bottom-left corner of cell (0,0)
   * @param cellWidth width of one cell in world units (&gt; 0)
   * @param cellHeight height of one cell in world units (&gt; 0)
   * @param columns number of columns (&gt; 0)
   * @param rows number of rows (&gt; 0)
   */
  public Grid(
      float originX, float originY, float cellWidth, float cellHeight, int columns, int rows) {
    if (cellWidth <= 0f || cellHeight <= 0f) {
      throw new IllegalArgumentException("cell size must be positive");
    }
    if (columns <= 0 || rows <= 0) {
      throw new IllegalArgumentException("columns and rows must be positive");
    }
    this.originX = originX;
    this.originY = originY;
    this.cellWidth = cellWidth;
    this.cellHeight = cellHeight;
    this.columns = columns;
    this.rows = rows;
  }

  public float originX() {
    return originX;
  }

  public float originY() {
    return originY;
  }

  public float cellWidth() {
    return cellWidth;
  }

  public float cellHeight() {
    return cellHeight;
  }

  public int columns() {
    return columns;
  }

  public int rows() {
    return rows;
  }

  public float worldWidth() {
    return columns * cellWidth;
  }

  public float worldHeight() {
    return rows * cellHeight;
  }

  /** World position of the <em>center</em> of cell {@code (col,row)} (allocates). */
  public Vector2 cellToWorld(int col, int row) {
    return cellToWorld(col, row, new Vector2());
  }

  /** World center of cell {@code (col,row)} written into {@code out}; returns {@code out}. */
  public Vector2 cellToWorld(int col, int row, Vector2 out) {
    Objects.requireNonNull(out, "out must not be null");
    out.x = originX + (col + 0.5f) * cellWidth;
    out.y = originY + (row + 0.5f) * cellHeight;
    return out;
  }

  /**
   * Cell containing world point {@code (wx,wy)}, or empty if out of bounds. Allocates; use {@link
   * #worldToCell(float, float, GridPoint2)} to avoid it.
   */
  public Optional<GridPoint2> worldToCell(float wx, float wy) {
    GridPoint2 out = new GridPoint2();
    return worldToCell(wx, wy, out) ? Optional.of(out) : Optional.empty();
  }

  /**
   * Writes the cell containing {@code (wx,wy)} into {@code out} and returns {@code true}; returns
   * {@code false} and leaves {@code out} unchanged when the point is outside the grid. Uses floor
   * semantics so a point exactly on a cell's lower/left edge belongs to that cell.
   */
  public boolean worldToCell(float wx, float wy, GridPoint2 out) {
    Objects.requireNonNull(out, "out must not be null");
    int col = MathUtils.floor((wx - originX) / cellWidth);
    int row = MathUtils.floor((wy - originY) / cellHeight);
    if (!inBounds(col, row)) return false;
    out.set(col, row);
    return true;
  }

  public boolean inBounds(int col, int row) {
    return col >= 0 && col < columns && row >= 0 && row < rows;
  }

  public boolean inBounds(GridPoint2 cell) {
    return cell != null && inBounds(cell.x, cell.y);
  }
}
