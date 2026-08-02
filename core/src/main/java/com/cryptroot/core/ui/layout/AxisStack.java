package com.cryptroot.core.ui.layout;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.cryptroot.core.ui.UiHelper;
import java.util.List;

/**
 * Shared implementation for the single-axis stacks, {@link VStack} and {@link HStack}.
 *
 * <p>Both do the same thing along different axes — measure children, hand out leftover space by
 * grow weight, then walk a cursor along the <em>main</em> axis while aligning or stretching each
 * child across the <em>cross</em> axis. Expressing that once in main/cross terms keeps the
 * arithmetic in one place, so a fix to the distribution or alignment rules cannot apply to only one
 * orientation.
 *
 * <p>Package-private: {@link VStack} and {@link HStack} are the public API.
 *
 * @param <SELF> the concrete stack type, so fluent setters keep their precise return type
 */
abstract class AxisStack<SELF extends AxisStack<SELF>> extends LayoutContainer<SELF> {

  /** Per-child measurements, grown on demand so a layout pass allocates nothing in steady state. */
  private float[] mainSize = new float[8];

  private float[] crossSize = new float[8];
  private float[] weights = new float[8];
  private float[] resolved = new float[8];

  /** {@code true} for a top-to-bottom stack, {@code false} for left-to-right. */
  abstract boolean vertical();

  // -------------------------------------------------------------------------
  // Measure
  // -------------------------------------------------------------------------

  /**
   * Natural size: children summed along the main axis (including {@linkplain #spacing() spacing})
   * and maxed along the cross axis, plus {@linkplain #padding() padding}.
   */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    List<LayoutElement> kids = managed();
    int n = kids.size();
    float mainTotal = n > 1 ? spacing() * (n - 1) : 0f;
    float crossMax = 0f;

    Vector2 tmp = measureScratch();
    for (LayoutElement kid : kids) {
      kid.preferredSize(tmp);
      mainTotal += vertical() ? tmp.y : tmp.x;
      crossMax = Math.max(crossMax, vertical() ? tmp.x : tmp.y);
    }

    Insets pad = padding();
    if (vertical()) {
      return out.set(crossMax + pad.horizontal(), mainTotal + pad.vertical());
    }
    return out.set(mainTotal + pad.horizontal(), crossMax + pad.vertical());
  }

  // -------------------------------------------------------------------------
  // Arrange
  // -------------------------------------------------------------------------

  @Override
  protected void arrange(Rectangle content) {
    List<LayoutElement> kids = managed();
    int n = kids.size();
    if (n == 0) return;
    ensureCapacity(n);

    boolean vert = vertical();
    float mainAvail = vert ? content.height : content.width;
    float crossAvail = vert ? content.width : content.height;

    // --- measure ---
    Vector2 tmp = measureScratch();
    for (int i = 0; i < n; i++) {
      LayoutElement kid = kids.get(i);
      kid.preferredSize(tmp);
      mainSize[i] = vert ? tmp.y : tmp.x;
      crossSize[i] = vert ? tmp.x : tmp.y;
      weights[i] = kid.growWeight();
    }

    // --- resolve final main-axis sizes ---
    resolveMainSizes(mainSize, weights, n, mainAvail, spacing(), resolved);

    float usedMain = spacing() * (n - 1);
    for (int i = 0; i < n; i++) usedMain += resolved[i];

    // --- position the block along the main axis when space is left over ---
    float slack = Math.max(0f, mainAvail - usedMain);
    float mainOffset = mainStartOffset(slack);

    float cursor =
        vert
            ? content.y + content.height - mainOffset // top-down
            : content.x + mainOffset; // left-to-right

    int crossAlign = crossAlignMask();
    for (int i = 0; i < n; i++) {
      float main = resolved[i];
      float cross = stretchCross() ? crossAvail : Math.min(crossSize[i], crossAvail);

      if (vert) {
        float x =
            stretchCross()
                ? content.x
                : UiHelper.alignIn(content.x, content.width, cross, crossAlign);
        cursor -= main;
        kids.get(i).setBounds(x, cursor, cross, main);
        cursor -= spacing();
      } else {
        float y =
            stretchCross()
                ? content.y
                : UiHelper.alignIn(
                    content.y, content.height, cross, verticalToHorizontal(crossAlign));
        kids.get(i).setBounds(cursor, y, main, cross);
        cursor += main + spacing();
      }
    }
  }

  // -------------------------------------------------------------------------
  // Main-axis sizing
  // -------------------------------------------------------------------------

  /**
   * Resolves each child's final main-axis size.
   *
   * <p>A child with <b>no</b> grow weight keeps its natural size. A child <b>with</b> a grow weight
   * is sized purely by its share of what is left after the unweighted children and the spacing have
   * been accounted for — its natural size is deliberately ignored.
   *
   * <p>That last point is the whole reason this is not a simple "add leftover on top of natural
   * size" distribution. If natural size still counted, three panels weighted {@code 1} each would
   * come out <em>unequal</em> whenever their contents differ in width, because each would keep its
   * own natural width and only share the remainder. Equal weights must mean equal columns, so
   * weighted children are measured from zero. A weighted child can therefore end up smaller than
   * its content, which is what {@linkplain LayoutContainer#clipChildren(boolean) clipping} is for.
   *
   * <p>The final weighted child receives whatever remains rather than its own rounded share, so the
   * total is exact and a row of equal columns has no leftover seam at the end.
   *
   * <p>With no weighted children at all, every child keeps its natural size and the stack may
   * overflow its container; overflow is preserved rather than shrunk so that content is never
   * silently squashed.
   *
   * @param natural per-child natural main-axis sizes
   * @param weights per-child grow weights
   * @param count number of valid entries in each array
   * @param available main-axis space in the content rectangle
   * @param spacing gap between adjacent children
   * @param out receives each child's resolved main-axis size; entries are overwritten
   */
  static void resolveMainSizes(
      float[] natural, float[] weights, int count, float available, float spacing, float[] out) {
    float totalWeight = 0f;
    int lastWeighted = -1;
    float unweightedTotal = count > 1 ? spacing * (count - 1) : 0f;

    for (int i = 0; i < count; i++) {
      if (weights[i] > 0f) {
        totalWeight += weights[i];
        lastWeighted = i;
      } else {
        unweightedTotal += natural[i];
      }
    }

    if (lastWeighted < 0) {
      System.arraycopy(natural, 0, out, 0, count);
      return;
    }

    float pool = Math.max(0f, available - unweightedTotal);
    float given = 0f;
    for (int i = 0; i < count; i++) {
      if (weights[i] <= 0f) {
        out[i] = natural[i];
      } else if (i == lastWeighted) {
        out[i] = Math.max(0f, pool - given);
      } else {
        out[i] = pool * (weights[i] / totalWeight);
        given += out[i];
      }
    }
  }

  // -------------------------------------------------------------------------
  // Alignment helpers
  // -------------------------------------------------------------------------

  /**
   * Distance from the main-axis start at which the block of children begins, given {@code slack}
   * unallocated space. A stack whose children fill the container (or overflow it) has no slack and
   * therefore no offset.
   */
  private float mainStartOffset(float slack) {
    if (slack <= 0f) return 0f;
    int a = align();
    if (vertical()) {
      if ((a & Align.bottom) != 0) return slack;
      if ((a & Align.top) != 0) return 0f;
    } else {
      if ((a & Align.right) != 0) return slack;
      if ((a & Align.left) != 0) return 0f;
    }
    return slack / 2f; // centre
  }

  /**
   * The half of {@link #align()} that applies across the stack: the horizontal flags for a vertical
   * stack, the vertical flags for a horizontal one.
   */
  private int crossAlignMask() {
    int a = align();
    return vertical() ? (a & (Align.left | Align.right)) : (a & (Align.top | Align.bottom));
  }

  /**
   * Re-expresses vertical align flags as the horizontal ones {@link UiHelper#alignIn} understands,
   * so one 1-D alignment helper serves both axes. {@code top} maps to "end of the axis" ({@code
   * right}) because both mean the high-coordinate edge in libGDX's Y-up space.
   */
  private static int verticalToHorizontal(int verticalAlign) {
    if ((verticalAlign & Align.bottom) != 0) return Align.left;
    if ((verticalAlign & Align.top) != 0) return Align.right;
    return Align.center;
  }

  private void ensureCapacity(int n) {
    if (mainSize.length >= n) return;
    int cap = Math.max(n, mainSize.length * 2);
    mainSize = new float[cap];
    crossSize = new float[cap];
    weights = new float[cap];
    resolved = new float[cap];
  }
}
