package com.cryptroot.core.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;

class NavigationGroupTest {

  private static final class FakeActivatable implements Activatable {
    boolean selected;
    int activations;

    @Override
    public void setSelected(boolean selected) {
      this.selected = selected;
    }

    @Override
    public void activate() {
      activations++;
    }
  }

  @Test
  void firstItemAddedIsAutoSelected() {
    NavigationGroup group = new NavigationGroup(NavigationGroup.Orientation.VERTICAL, true);
    FakeActivatable a = new FakeActivatable();
    group.add(a);

    assertEquals(0, group.selectedIndex());
    assertTrue(a.selected);
  }

  @Test
  void downMovesSelectionAndTogglesFlags() {
    NavigationGroup group = new NavigationGroup(NavigationGroup.Orientation.VERTICAL, true);
    FakeActivatable a = new FakeActivatable();
    FakeActivatable b = new FakeActivatable();
    group.addAll(java.util.List.of(a, b));

    boolean consumed = group.keyDown(Input.Keys.DOWN);

    assertTrue(consumed);
    assertEquals(1, group.selectedIndex());
    assertFalse(a.selected);
    assertTrue(b.selected);
  }

  @Test
  void wrapsAroundWhenEnabled() {
    NavigationGroup group = new NavigationGroup(NavigationGroup.Orientation.VERTICAL, true);
    FakeActivatable a = new FakeActivatable();
    FakeActivatable b = new FakeActivatable();
    group.addAll(java.util.List.of(a, b));

    group.keyDown(Input.Keys.UP);

    assertEquals(1, group.selectedIndex(), "moving up from index 0 should wrap to the last item");
  }

  @Test
  void clampsWhenWrapDisabled() {
    NavigationGroup group = new NavigationGroup(NavigationGroup.Orientation.VERTICAL, false);
    FakeActivatable a = new FakeActivatable();
    FakeActivatable b = new FakeActivatable();
    group.addAll(java.util.List.of(a, b));

    group.keyDown(Input.Keys.UP);

    assertEquals(0, group.selectedIndex());
  }

  @Test
  void horizontalGroupIgnoresVerticalKeys() {
    NavigationGroup group = new NavigationGroup(NavigationGroup.Orientation.HORIZONTAL, true);
    group.add(new FakeActivatable());

    assertFalse(group.keyDown(Input.Keys.DOWN));
    assertFalse(group.keyDown(Input.Keys.UP));
  }

  @Test
  void verticalGroupIgnoresHorizontalKeys() {
    NavigationGroup group = new NavigationGroup(NavigationGroup.Orientation.VERTICAL, true);
    group.add(new FakeActivatable());

    assertFalse(group.keyDown(Input.Keys.LEFT));
    assertFalse(group.keyDown(Input.Keys.RIGHT));
  }

  @Test
  void enterActivatesSelection() {
    NavigationGroup group = new NavigationGroup(NavigationGroup.Orientation.VERTICAL, true);
    FakeActivatable a = new FakeActivatable();
    group.add(a);

    boolean consumed = group.keyDown(Input.Keys.ENTER);

    assertTrue(consumed);
    assertEquals(1, a.activations);
  }

  @Test
  void unhandledKeyReturnsFalse() {
    NavigationGroup group = new NavigationGroup(NavigationGroup.Orientation.VERTICAL, true);
    group.add(new FakeActivatable());

    assertFalse(group.keyDown(Input.Keys.ESCAPE));
  }

  @Test
  void emptyGroupActivateAndMoveAreNoOps() {
    NavigationGroup group = new NavigationGroup(NavigationGroup.Orientation.VERTICAL, true);

    assertEquals(-1, group.selectedIndex());
    group.activate();
    assertTrue(group.keyDown(Input.Keys.DOWN));
    assertEquals(-1, group.selectedIndex());
  }

  @Test
  void clearDeselectsAndEmptiesGroup() {
    NavigationGroup group = new NavigationGroup(NavigationGroup.Orientation.VERTICAL, true);
    FakeActivatable a = new FakeActivatable();
    group.add(a);

    group.clear();

    assertFalse(a.selected);
    assertEquals(0, group.size());
    assertEquals(-1, group.selectedIndex());
  }
}
