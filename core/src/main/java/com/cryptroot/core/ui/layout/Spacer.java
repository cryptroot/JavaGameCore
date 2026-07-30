package com.cryptroot.core.ui.layout;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Vector2;

/**
 * An invisible, non-interactive gap in a stack.
 *
 * <p>Two uses:
 *
 * <ul>
 *   <li><b>Fixed gap</b> — {@code new Spacer(24f, 0f)} inserts 24 units, for a one-off separation
 *       that does not warrant changing the whole stack's {@linkplain LayoutContainer#spacing(float)
 *       spacing}.
 *   <li><b>Flexible gap</b> — {@code new Spacer(0f, 1f)} soaks up leftover space, which is how you
 *       push subsequent children to the far end of a stack. A spacer between two groups splits a
 *       row into left-aligned and right-aligned halves without either group knowing the container's
 *       width.
 * </ul>
 *
 * <pre>{@code
 * // "Day 1  Time 08:00" on the left, action buttons pushed to the right:
 * new HStack().spacing(8f)
 *         .add(dayLabel)
 *         .add(timeLabel)
 *         .add(new Spacer(0f, 1f))
 *         .add(startDayButton)
 *         .add(stepButton);
 * }</pre>
 */
public final class Spacer implements LayoutElement {

  private final float minSize;
  private final float weight;

  /**
   * @param minSize natural size along both axes; clamped to at least 0
   * @param weight share of the container's leftover space; clamped to at least 0
   */
  public Spacer(float minSize, float weight) {
    this.minSize = Math.max(0f, minSize);
    this.weight = Math.max(0f, weight);
  }

  /** A fixed-size gap with no grow weight. */
  public static Spacer of(float size) {
    return new Spacer(size, 0f);
  }

  /** A zero-size gap that absorbs all leftover space, pushing later children to the far end. */
  public static Spacer flexible() {
    return new Spacer(0f, 1f);
  }

  @Override
  public Vector2 preferredSize(Vector2 out) {
    return out.set(minSize, minSize);
  }

  @Override
  public float growWeight() {
    return weight;
  }

  /** Nothing to store: a spacer occupies space by being measured, and never draws. */
  @Override
  public void setBounds(float x, float y, float width, float height) {}

  @Override
  public void layout() {}

  @Override
  public void updateHover(float worldX, float worldY) {}

  @Override
  public boolean hit(float worldX, float worldY) {
    return false;
  }

  @Override
  public boolean update(float delta) {
    return false;
  }

  @Override
  public void draw(PolygonSpriteBatch batch) {}

  @Override
  public void reset() {}
}
