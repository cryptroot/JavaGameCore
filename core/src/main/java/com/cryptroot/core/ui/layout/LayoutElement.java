package com.cryptroot.core.ui.layout;

import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.ui.UiWidget;

/**
 * A {@link UiWidget} that a {@link LayoutContainer} can measure and place.
 *
 * <h3>The geometry contract</h3>
 *
 * Geometry is <em>always</em> the widget's outer rectangle, with a bottom-left origin, in world
 * units (libGDX Y-up). Never a text baseline, never a centre point, never an inner content box.
 *
 * <p>This is the whole reason the interface exists. {@link UiWidget#setPosition(float, float)}
 * alone is not enough to build containers on, because its meaning was never pinned down: it is a
 * no-op by default, it moves a text baseline on some widgets and a bottom-left corner on others,
 * and several widgets ignore it entirely. A container cannot align children whose origins mean
 * different things, so anything it lays out must implement this interface instead.
 *
 * <h3>Measure and arrange</h3>
 *
 * Layout is two ordinary method calls, not a separate lifecycle phase:
 *
 * <ol>
 *   <li><b>Measure</b> — the container calls {@link #preferredSize(Vector2)}, which recurses into
 *       children as needed, to learn each child's natural size.
 *   <li><b>Arrange</b> — the container calls {@link #setBounds} on each child, then the existing
 *       {@link UiWidget#layout()} cascade applies it.
 * </ol>
 *
 * <p>{@code setBounds} therefore only records the rectangle; it must not compute derived geometry.
 * That happens in {@code layout()}, which the container's own {@code layout()} always invokes
 * afterwards.
 *
 * <h3>Migration</h3>
 *
 * Containers fall back to {@link UiWidget#setPosition} for children that do not implement this
 * interface, so widgets can be converted one at a time. Most inherit a correct implementation
 * simply by extending {@link com.cryptroot.core.ui.BoundedWidget BoundedWidget}.
 */
public interface LayoutElement extends UiWidget {

  /**
   * Writes this widget's natural size — the size it wants when nothing constrains it — into {@code
   * out} and returns it.
   *
   * <p>Takes an output parameter because containers call this once per child per layout pass and
   * must not allocate on that path.
   *
   * @param out scratch vector to write into; must not be null
   * @return {@code out}, for chaining
   */
  Vector2 preferredSize(Vector2 out);

  /**
   * Records this widget's outer rectangle. Applied by the next {@link UiWidget#layout()} call,
   * which a {@link LayoutContainer} always makes after arranging its children.
   *
   * @param x left edge in world units
   * @param y bottom edge in world units
   * @param width outer width
   * @param height outer height
   */
  void setBounds(float x, float y, float width, float height);

  /**
   * This widget's share of a container's leftover space along the main axis.
   *
   * <p>Zero (the default) means "natural size only". Positive weights split whatever space remains
   * after every child's natural size has been allocated, in proportion to the weights: two children
   * weighted {@code 1} and {@code 2} receive one third and two thirds of the remainder.
   */
  default float growWeight() {
    return 0f;
  }

  /**
   * Natural height when the width is already fixed to {@code width} — the escape hatch for content
   * whose height depends on its width, such as wrapped text.
   *
   * <p>The default ignores {@code width} and returns {@link #preferredSize}'s height, which is
   * correct for every widget whose size is width-independent.
   *
   * @param width the width the widget will be given
   * @param scratch scratch vector the implementation may use; contents are not meaningful on return
   */
  default float preferredHeightForWidth(float width, Vector2 scratch) {
    return preferredSize(scratch).y;
  }
}
