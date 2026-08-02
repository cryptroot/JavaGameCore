package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.event.Signal;
import com.cryptroot.core.ui.layout.VStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
public final class RadioGroup extends BoundedWidget {

  /** Fires with the index of the newly selected item on every selection change. */
  public final Signal<Integer> onSelectionChanged = new Signal<>();

  /**
   * Typed list of checkboxes — kept alongside the base-class children list so mutual-exclusion
   * logic can call Checkbox-specific methods.
   */
  private final List<Checkbox> checkboxes;

  /**
   * The actual vertical arrangement, delegated to {@link VStack} rather than re-implemented.
   *
   * <p>This class previously stacked its checkboxes with its own {@code startY - i * spacing}
   * arithmetic, which is exactly what a stack container does — including getting row heights from
   * the children instead of from a guessed pitch.
   */
  private final VStack stack = new VStack();

  private int selectedIndex = 0;

  /**
   * Creates a group sized by its enclosing layout container.
   *
   * @param skin skin shared by all checkboxes (font defines box size)
   * @param pixel 1×1 white texture passed to each {@link Checkbox}
   * @param labels option labels, one per checkbox
   * @param spacing vertical gap between consecutive options
   */
  public RadioGroup(UiSkin skin, Texture pixel, List<String> labels, float spacing) {
    Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(pixel, "pixel must not be null");
    Objects.requireNonNull(labels, "labels must not be null");
    if (labels.isEmpty()) {
      throw new IllegalArgumentException("labels must not be empty");
    }
    stack.spacing(spacing).stretchCross(true);
    addChild(stack);

    checkboxes = new ArrayList<>(labels.size());
    for (int i = 0; i < labels.size(); i++) {
      boolean initial = (i == 0);
      Checkbox cb = new Checkbox(skin, pixel, labels.get(i), initial);
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
      stack.add(cb);
    }
  }

  /**
   * Creates a group whose top-left corner is at {@code (x, startY)}, for hand-positioned screens.
   *
   * @param spacing vertical gap between consecutive options
   */
  public RadioGroup(
      UiSkin skin, Texture pixel, List<String> labels, float x, float startY, float spacing) {
    this(skin, pixel, labels, spacing);
    Vector2 natural = preferredSize(new Vector2());
    setBounds(x, startY - natural.y, natural.x, natural.y);
  }

  // -------------------------------------------------------------------------
  // BoundedWidget
  // -------------------------------------------------------------------------

  /** Natural size: whatever the internal stack needs. */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    return stack.preferredSize(out);
  }

  @Override
  protected void doBoundedLayout() {
    if (frame.width <= 0f || frame.height <= 0f) {
      Vector2 natural = stack.preferredSize(scratch);
      if (frame.width <= 0f) frame.width = natural.x;
      if (frame.height <= 0f) frame.height = natural.y;
    }
    bounds.set(frame);
    stack.setBounds(frame.x, frame.y, frame.width, frame.height);
  }

  /** Scratch for measuring, so layout allocates nothing. */
  private final Vector2 scratch = new Vector2();

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
