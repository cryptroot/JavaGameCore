package com.cryptroot.core.uitest;

import com.cryptroot.core.ui.BoundedWidget;
import com.cryptroot.core.ui.Button;
import com.cryptroot.core.ui.CompositeWidget;
import com.cryptroot.core.ui.ScrollList;
import com.cryptroot.core.ui.TextLabel;
import com.cryptroot.core.ui.UiLayer;
import com.cryptroot.core.ui.UiWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Finds widgets in a live {@link UiLayer} by what is on screen — label text, type — rather than by
 * a reference the test holds.
 *
 * <h3>Why by text and not by reference</h3>
 *
 * A screen that repopulates itself rebuilds its widgets. The usual way to refresh a data-driven
 * panel is to clear its row container and recreate a {@link Button} per item, so a reference
 * captured before an interaction points at a widget that is no longer in the tree: clicks on it hit
 * nothing and nothing fails loudly. Resolving the target at the moment of the click is the only
 * approach that survives that, and it also makes a scenario read like the UI ("click Confirm")
 * instead of like the implementation.
 *
 * <p>Traversal is depth-first over {@link UiLayer#widgets()} (which includes the layout root) and
 * {@link CompositeWidget#children()}, skipping subtrees whose root is {@linkplain
 * UiWidget#isVisible() invisible} — a hidden dialog must not be clickable.
 */
public final class WidgetQuery {

  private WidgetQuery() {}

  /** Every visible widget in the layer, depth-first, parents before children. */
  public static List<UiWidget> flatten(UiLayer layer) {
    Objects.requireNonNull(layer, "layer must not be null");
    List<UiWidget> out = new ArrayList<>();
    for (UiWidget widget : layer.widgets()) {
      collect(widget, out);
    }
    return out;
  }

  private static void collect(UiWidget widget, List<UiWidget> out) {
    if (!widget.isVisible()) return;
    out.add(widget);
    if (widget instanceof CompositeWidget composite) {
      for (UiWidget child : composite.children()) {
        collect(child, out);
      }
    }
  }

  /** Every visible widget of the given type, depth-first. */
  public static <T> List<T> allOfType(UiLayer layer, Class<T> type) {
    Objects.requireNonNull(type, "type must not be null");
    List<T> out = new ArrayList<>();
    for (UiWidget widget : flatten(layer)) {
      if (type.isInstance(widget)) {
        out.add(type.cast(widget));
      }
    }
    return out;
  }

  /**
   * The text of a {@link Button}'s label.
   *
   * <p>{@code Button} keeps its {@link TextLabel} private but adds it as a child, so the label is
   * read back out of the widget tree rather than through an accessor {@code core} does not need to
   * grow.
   *
   * @return the label text, or {@code ""} if the button somehow has no text child
   */
  public static String labelOf(Button button) {
    Objects.requireNonNull(button, "button must not be null");
    for (UiWidget child : button.children()) {
      if (child instanceof TextLabel label) {
        return label.getText();
      }
    }
    return "";
  }

  /** Every visible button's label text, in traversal order. Used to build failure messages. */
  public static List<String> buttonLabels(UiLayer layer) {
    List<String> out = new ArrayList<>();
    for (Button button : allOfType(layer, Button.class)) {
      out.add(labelOf(button));
    }
    return out;
  }

  /**
   * Every visible {@link TextLabel}'s text, plus every {@link ScrollList} row, in traversal order.
   */
  public static List<String> allText(UiLayer layer) {
    List<String> out = new ArrayList<>();
    for (UiWidget widget : flatten(layer)) {
      if (widget instanceof TextLabel label) {
        out.add(label.getText());
      } else if (widget instanceof ScrollList list) {
        out.addAll(list.getItems());
      }
    }
    return out;
  }

  /** Whether any visible label or list row equals {@code text}. */
  public static boolean textVisible(UiLayer layer, String text) {
    Objects.requireNonNull(text, "text must not be null");
    return allText(layer).contains(text);
  }

  /** Whether any visible label or list row contains {@code fragment}. */
  public static boolean textContaining(UiLayer layer, String fragment) {
    Objects.requireNonNull(fragment, "fragment must not be null");
    return allText(layer).stream().anyMatch(t -> t.contains(fragment));
  }

  /** The first visible button whose label equals {@code label}. */
  public static Optional<Button> button(UiLayer layer, String label) {
    Objects.requireNonNull(label, "label must not be null");
    return allOfType(layer, Button.class).stream()
        .filter(b -> labelOf(b).equals(label))
        .findFirst();
  }

  /**
   * The first visible button whose label contains {@code fragment}.
   *
   * <p>The fragment form is the practical one for data rows, whose text is assembled from a
   * localisation pattern ({@code "{0} [{1}] {2}"}) and so carries ids, states and stray whitespace
   * a test should not have to reproduce exactly.
   */
  public static Optional<Button> buttonContaining(UiLayer layer, String fragment) {
    Objects.requireNonNull(fragment, "fragment must not be null");
    return allOfType(layer, Button.class).stream()
        .filter(b -> labelOf(b).contains(fragment))
        .findFirst();
  }

  /**
   * As {@link #button(UiLayer, String)}, but fails with the list of labels that <em>were</em> on
   * screen.
   *
   * <p>That message is the difference between a five-minute and an hour-long diagnosis: a missing
   * button and a button whose text changed look identical from the caller's side.
   *
   * @throws AssertionError if no visible button carries that exact label
   */
  public static Button requireButton(UiLayer layer, String label) {
    return button(layer, label)
        .orElseThrow(() -> new AssertionError(notFoundMessage(layer, "labelled '" + label + "'")));
  }

  /**
   * As {@link #buttonContaining(UiLayer, String)}, but fails with the list of labels that
   * <em>were</em> on screen.
   *
   * @throws AssertionError if no visible button's label contains {@code fragment}
   */
  public static Button requireButtonContaining(UiLayer layer, String fragment) {
    return buttonContaining(layer, fragment)
        .orElseThrow(
            () -> new AssertionError(notFoundMessage(layer, "containing '" + fragment + "'")));
  }

  /**
   * The only visible widget of {@code type}, for screens with a single list, field or slider.
   *
   * @throws AssertionError if there is not exactly one
   */
  public static <T extends BoundedWidget> T requireSingle(UiLayer layer, Class<T> type) {
    List<T> found = allOfType(layer, type);
    if (found.size() != 1) {
      throw new AssertionError(
          "expected exactly one visible " + type.getSimpleName() + ", found " + found.size());
    }
    return found.get(0);
  }

  private static String notFoundMessage(UiLayer layer, String wanted) {
    return "no visible Button " + wanted + "; visible button labels were " + buttonLabels(layer);
  }
}
