package com.cryptroot.core.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Keyboard-only menu navigation over an ordered list of {@link Activatable} widgets — arrow keys
 * move the selection, Enter/Space activates it.
 *
 * <p>There is deliberately no mouse-hover synchronisation: a group never reads {@link
 * BoundedWidget#hovered} or similar. Keeping keyboard and pointer input fully decoupled keeps this
 * class GL-free and unit-testable with a fake {@link Activatable}, and avoids the keyboard
 * selection jumping around whenever the pointer merely passes over a widget.
 *
 * <p>Wire a group's {@link #inputAdapter()} into a screen's {@code inputProcessor()} (composed with
 * the screen's other adapters via {@code InputMultiplexer}); the group returns {@code false} for
 * every key it does not handle so the multiplexer's remaining processors — e.g. a screen's own
 * ESCAPE-to-back handler — still receive it.
 *
 * <pre>{@code
 * NavigationGroup nav = new NavigationGroup(NavigationGroup.Orientation.VERTICAL, true);
 * nav.add(newGameButton);
 * nav.add(quitButton);
 * }</pre>
 */
public final class NavigationGroup {

  public enum Orientation {
    VERTICAL,
    HORIZONTAL
  }

  private final List<Activatable> items = new ArrayList<>();
  private final Orientation orientation;
  private final boolean wrap;
  private int selectedIndex = -1;

  public NavigationGroup(Orientation orientation, boolean wrap) {
    this.orientation = Objects.requireNonNull(orientation, "orientation must not be null");
    this.wrap = wrap;
  }

  /** Appends {@code item}, selecting it if the group was empty. */
  public void add(Activatable item) {
    Objects.requireNonNull(item, "item must not be null");
    items.add(item);
    if (selectedIndex < 0) select(0);
  }

  public void addAll(List<? extends Activatable> newItems) {
    Objects.requireNonNull(newItems, "newItems must not be null");
    newItems.forEach(this::add);
  }

  /** Removes every item and clears the selection. */
  public void clear() {
    deselectCurrent();
    items.clear();
    selectedIndex = -1;
  }

  public int size() {
    return items.size();
  }

  /** The current selection, or -1 if the group is empty. */
  public int selectedIndex() {
    return selectedIndex;
  }

  /** Selects {@code index}, clamped to the valid range; a no-op on an empty group. */
  public void select(int index) {
    if (items.isEmpty()) {
      selectedIndex = -1;
      return;
    }
    deselectCurrent();
    selectedIndex = Math.max(0, Math.min(index, items.size() - 1));
    items.get(selectedIndex).setSelected(true);
  }

  /** Moves the selection forward, wrapping or clamping per the constructor's {@code wrap} flag. */
  public void next() {
    move(1);
  }

  /** Moves the selection backward, wrapping or clamping per the constructor's {@code wrap} flag. */
  public void previous() {
    move(-1);
  }

  /** Activates the current selection; a no-op if nothing is selected. */
  public void activate() {
    if (selectedIndex >= 0 && selectedIndex < items.size()) {
      items.get(selectedIndex).activate();
    }
  }

  /**
   * Handles a key press, returning {@code true} if this group consumed it. {@link
   * Orientation#VERTICAL} responds to UP/DOWN, {@link Orientation#HORIZONTAL} to LEFT/RIGHT;
   * ENTER/NUMPAD_ENTER/SPACE always activate the selection regardless of orientation. Every other
   * key returns {@code false} so it still reaches the rest of an {@code InputMultiplexer} chain.
   */
  public boolean keyDown(int keycode) {
    return switch (keycode) {
      case Input.Keys.UP -> respond(Orientation.VERTICAL, this::previous);
      case Input.Keys.DOWN -> respond(Orientation.VERTICAL, this::next);
      case Input.Keys.LEFT -> respond(Orientation.HORIZONTAL, this::previous);
      case Input.Keys.RIGHT -> respond(Orientation.HORIZONTAL, this::next);
      case Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER, Input.Keys.SPACE -> {
        activate();
        yield true;
      }
      default -> false;
    };
  }

  /** An {@link InputAdapter} delegating {@code keyDown} to {@link #keyDown(int)}. */
  public InputAdapter inputAdapter() {
    return new InputAdapter() {
      @Override
      public boolean keyDown(int keycode) {
        return NavigationGroup.this.keyDown(keycode);
      }
    };
  }

  private boolean respond(Orientation forOrientation, Runnable move) {
    if (orientation != forOrientation) return false;
    move.run();
    return true;
  }

  private void move(int delta) {
    if (items.isEmpty()) return;
    int next = selectedIndex + delta;
    next = wrap ? Math.floorMod(next, items.size()) : Math.max(0, Math.min(next, items.size() - 1));
    select(next);
  }

  private void deselectCurrent() {
    if (selectedIndex >= 0 && selectedIndex < items.size()) {
      items.get(selectedIndex).setSelected(false);
    }
  }
}
