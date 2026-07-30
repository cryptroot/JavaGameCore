package com.cryptroot.core.ui.layout;

/**
 * Stacks children vertically, first child at the top.
 *
 * <p>Each child is given its natural height plus any share of leftover space its {@linkplain
 * LayoutElement#growWeight() grow weight} earns, with {@linkplain #spacing(float) spacing} between
 * adjacent children. Across the stack, children are either {@linkplain #stretchCross(boolean)
 * stretched} to the full width or aligned within it by the horizontal half of {@link #align(int)}.
 *
 * <p>Because heights come from the children themselves, a column of buttons cannot overlap — which
 * is the failure mode of choosing a row pitch by hand and hoping it exceeds the widget height.
 *
 * <pre>{@code
 * VStack rows = new VStack().spacing(6f);
 * for (Room room : rooms) {
 *     rows.add(new Button(rowSkin, room.label()));
 * }
 * }</pre>
 */
public final class VStack extends AxisStack<VStack> {

  @Override
  boolean vertical() {
    return true;
  }
}
