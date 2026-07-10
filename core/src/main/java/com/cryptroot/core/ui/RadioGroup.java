package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Texture;
import com.cryptroot.core.event.Signal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A mutually-exclusive group of {@link Checkbox} instances, itself a {@link CompositeWidget} so it
 * can be added to a {@link UiLayer} as a single unit.
 *
 * <p>When one checkbox is checked, all others are unchecked automatically. Use {@link
 * #addTo(UiLayer, int)} or {@code uiLayer.add(group, z)} directly:
 *
 * <pre>{@code
 * RadioGroup group = new RadioGroup(
 *     skin, pixel,
 *     List.of("Option A", "Option B", "Option C"),
 *     80f, 500f, 60f);
 * group.onSelectionChanged.connect(idx -> statusLabel = "Selected: " + idx);
 * group.addTo(uiLayer, 0);
 * }</pre>
 *
 * <p>The first item is selected by default. Use {@link #select(int)} to change the selection
 * programmatically without emitting {@link #onSelectionChanged}.
 */
public final class RadioGroup extends CompositeWidget {

  /** Fires with the index of the newly selected item on every selection change. */
  public final Signal<Integer> onSelectionChanged = new Signal<>();

  /**
   * Typed list of checkboxes — kept alongside the base-class children list so mutual-exclusion
   * logic can call Checkbox-specific methods.
   */
  private final List<Checkbox> checkboxes;

  private final float x;
  private final float startY;
  private final float spacing;
  private int selectedIndex = 0;

  /**
   * @param skin skin shared by all checkboxes (font defines box size)
   * @param pixel 1×1 white texture passed to each {@link Checkbox}
   * @param labels option labels, one per checkbox
   * @param x left edge of all checkboxes in world coordinates
   * @param startY text baseline of the first (topmost) checkbox
   * @param spacing vertical distance between consecutive baselines
   */
  public RadioGroup(
      UiSkin skin, Texture pixel, List<String> labels, float x, float startY, float spacing) {
    this.x = x;
    this.startY = startY;
    this.spacing = spacing;
    checkboxes = new ArrayList<>(labels.size());
    for (int i = 0; i < labels.size(); i++) {
      boolean initial = (i == 0);
      Checkbox cb = new Checkbox(skin, pixel, labels.get(i), x, startY - i * spacing, initial);
      final int index = i;
      cb.onChanged.connect(
          checked -> {
            if (checked) {
              // Uncheck all others silently, then emit selection change.
              for (int j = 0; j < checkboxes.size(); j++) {
                if (j != index) checkboxes.get(j).setCheckedSilent(false);
              }
              selectedIndex = index;
              onSelectionChanged.emit(selectedIndex);
            } else {
              // Prevent unchecking the last checked item — re-check it silently.
              cb.setCheckedSilent(true);
            }
          });
      checkboxes.add(cb);
      addChild(cb);
    }
  }

  // -------------------------------------------------------------------------
  // CompositeWidget
  // -------------------------------------------------------------------------

  /**
   * Repositions each checkbox along the vertical stack. Called automatically by {@link
   * CompositeWidget#layout()} before children lay themselves out, so a viewport resize correctly
   * re-flows the group.
   */
  @Override
  protected void doLayout() {
    for (int i = 0; i < checkboxes.size(); i++) {
      checkboxes.get(i).setPosition(x, startY - i * spacing);
    }
  }

  // doDraw() not needed — children draw themselves.
  // doReset() not needed — children reset themselves.

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Adds this group to {@code layer} as a single composite widget. Equivalent to {@code
   * layer.add(this, zOrder)}.
   */
  public void addTo(UiLayer layer, int zOrder) {
    layer.add(this, zOrder);
  }

  /** Returns an unmodifiable view of the checkbox instances in declaration order. */
  public List<Checkbox> buttons() {
    return Collections.unmodifiableList(checkboxes);
  }

  /** Returns the index of the currently selected item. */
  public int selectedIndex() {
    return selectedIndex;
  }

  /**
   * Programmatically selects item at {@code index} without emitting {@link #onSelectionChanged}.
   * Useful for restoring saved state.
   */
  public void select(int index) {
    if (index < 0 || index >= checkboxes.size()) return;
    for (int i = 0; i < checkboxes.size(); i++) {
      checkboxes.get(i).setCheckedSilent(i == index);
    }
    selectedIndex = index;
  }
}
