package com.cryptroot.core.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.ui.layout.VStack;
import org.junit.jupiter.api.Test;

/**
 * {@link CompositeWidget#hitFocusable()} must reach a {@link Focusable} nested at any depth.
 *
 * <p>{@link UiLayer} asks only the <em>top-level</em> widget which {@link Focusable} to focus, so a
 * container that answers "none" leaves every nested focusable widget — an {@link InputField} inside
 * a {@link VStack} inside a {@link Panel}, i.e. every layout-driven screen — permanently
 * unfocusable and therefore unable to receive a keystroke. That is what happened until containers
 * started recording which child consumed the hit.
 *
 * <p>Fakes rather than real widgets: {@code InputField} and {@code Panel} need a font and a
 * texture, and what is under test is the propagation, not the widgets.
 */
class CompositeWidgetFocusTest {

  /** A focusable leaf hit inside its assigned rectangle. */
  private static final class FocusableLeaf extends BoundedWidget implements Focusable {

    FocusableLeaf(float x, float y, float width, float height) {
      setBounds(x, y, width, height);
      layout();
    }

    @Override
    public void keyTyped(char character) {}

    @Override
    public boolean focusedKeyDown(int keycode) {
      return false;
    }

    @Override
    public void onFocusGained() {}

    @Override
    public void onFocusLost() {}
  }

  /** A non-focusable leaf that consumes hits inside its assigned rectangle. */
  private static final class PlainLeaf extends BoundedWidget {

    PlainLeaf(float x, float y, float width, float height) {
      setBounds(x, y, width, height);
      layout();
    }
  }

  /** A container with no geometry of its own, standing in for a panel or a stack. */
  private static class Group extends CompositeWidget {

    Group(UiWidget... kids) {
      for (UiWidget kid : kids) addChild(kid);
    }

    @Override
    protected void doLayout() {}

    @Override
    protected void doReset() {}

    @Override
    public void updateHover(float worldX, float worldY) {}
  }

  /** A container that is itself focusable, like an editable list would be. */
  private static final class FocusableGroup extends Group implements Focusable {

    FocusableGroup(UiWidget... kids) {
      super(kids);
    }

    @Override
    public void keyTyped(char character) {}

    @Override
    public boolean focusedKeyDown(int keycode) {
      return false;
    }

    @Override
    public void onFocusGained() {}

    @Override
    public void onFocusLost() {}
  }

  @Test
  void focusReachesADirectFocusableChild() {
    FocusableLeaf field = new FocusableLeaf(0f, 0f, 10f, 10f);
    Group group = new Group(field);

    assertTrue(group.hit(5f, 5f));
    assertSame(field, group.hitFocusable());
  }

  @Test
  void focusReachesAFocusableNestedInsideARealLayoutContainer() {
    // A VStack in the chain on purpose: LayoutContainer overrides hit() with its own clip check, so
    // its scan has to record the consuming child too.
    FocusableLeaf field = new FocusableLeaf(0f, 0f, 10f, 10f);
    VStack stack = new VStack();
    stack.add(field);
    stack.setBounds(0f, 0f, 10f, 10f);
    stack.layout();
    Group outer = new Group(stack);

    assertTrue(outer.hit(5f, 5f));
    assertSame(field, outer.hitFocusable());
  }

  @Test
  void hittingANonFocusableSiblingClearsFocus() {
    FocusableLeaf field = new FocusableLeaf(0f, 0f, 10f, 10f);
    PlainLeaf plain = new PlainLeaf(20f, 0f, 10f, 10f);
    Group group = new Group(field, plain);

    assertTrue(group.hit(5f, 5f));
    assertSame(field, group.hitFocusable());

    assertTrue(group.hit(25f, 5f));
    assertNull(group.hitFocusable(), "the plain sibling must not leave the field focused");
  }

  @Test
  void missingEverythingReportsNoFocusable() {
    Group group = new Group(new FocusableLeaf(0f, 0f, 10f, 10f));

    assertFalse(group.hit(100f, 100f));
    assertNull(group.hitFocusable());
  }

  @Test
  void resetForgetsTheLastHitChild() {
    FocusableLeaf field = new FocusableLeaf(0f, 0f, 10f, 10f);
    Group group = new Group(field);

    assertTrue(group.hit(5f, 5f));
    group.reset();

    assertNull(group.hitFocusable(), "a re-entered screen must not resurrect stale focus");
  }

  @Test
  void aFocusableContainerReportsItselfRatherThanAChild() {
    FocusableLeaf child = new FocusableLeaf(0f, 0f, 10f, 10f);
    FocusableGroup group = new FocusableGroup(child);

    assertTrue(group.hit(5f, 5f));
    assertSame(group, group.hitFocusable(), "an outer focusable widget owns the focus");
  }
}
