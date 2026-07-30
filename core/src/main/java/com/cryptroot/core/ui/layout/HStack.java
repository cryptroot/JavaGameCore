package com.cryptroot.core.ui.layout;

/**
 * Stacks children horizontally, first child on the left.
 *
 * <p>Each child is given its natural width plus any share of leftover space its {@linkplain
 * LayoutElement#growWeight() grow weight} earns, with {@linkplain #spacing(float) spacing} between
 * adjacent children. Across the stack, children are either {@linkplain #stretchCross(boolean)
 * stretched} to the full height or aligned within it by the vertical half of {@link #align(int)}.
 *
 * <p>Equal grow weights produce equal columns that exactly fill the available width at any
 * resolution — the replacement for hand-computed column positions.
 *
 * <pre>{@code
 * HStack columns = new HStack().spacing(16f).stretchCross(true)
 *         .add(roomsPanel, 1f)
 *         .add(guestsPanel, 1f)
 *         .add(employeesPanel, 1f);
 * }</pre>
 */
public final class HStack extends AxisStack<HStack> {

  @Override
  boolean vertical() {
    return false;
  }
}
