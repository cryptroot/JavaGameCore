package com.cryptroot.core.ui;

import com.cryptroot.core.ui.layout.Insets;

/**
 * The shared spacing and metric scale for a {@link UiSkin}.
 *
 * <p>Exists so that padding is defined once instead of as a private constant inside each widget.
 * Before this, every widget carried its own {@code static final float PAD_H}/{@code PAD_V} pair
 * with no relationship between them, which made consistent spacing impossible to achieve and
 * impossible to retune — and left the toolkit with hand-computed fudge factors like a hardcoded
 * "approximate half-cap-height of a body font".
 *
 * <p>Reached through {@link UiSkin#theme()}, so it travels with the skin a widget was constructed
 * with and needs no separate plumbing.
 *
 * @param gapTight tight spacing, e.g. between an icon and its label
 * @param gap default spacing between sibling widgets
 * @param gapLoose loose spacing, e.g. between groups of widgets
 * @param controlPadding padding between a control's border and its text (buttons, fields, list
 *     rows)
 * @param panelPadding padding between a panel's border and its content
 * @param rowHeight standard height of a single-line interactive row
 * @param borderThickness default border stroke width
 * @param minControlWidth minimum width for a control whose natural width would be uncomfortably
 *     small
 */
public record UiTheme(
    float gapTight,
    float gap,
    float gapLoose,
    Insets controlPadding,
    Insets panelPadding,
    float rowHeight,
    float borderThickness,
    float minControlWidth) {

  public UiTheme {
    if (gapTight < 0f || gap < 0f || gapLoose < 0f) {
      throw new IllegalArgumentException("gaps must not be negative");
    }
    if (rowHeight <= 0f) {
      throw new IllegalArgumentException("rowHeight must be positive, got " + rowHeight);
    }
    if (borderThickness < 0f) {
      throw new IllegalArgumentException(
          "borderThickness must not be negative, got " + borderThickness);
    }
    if (minControlWidth < 0f) {
      throw new IllegalArgumentException(
          "minControlWidth must not be negative, got " + minControlWidth);
    }
    java.util.Objects.requireNonNull(controlPadding, "controlPadding must not be null");
    java.util.Objects.requireNonNull(panelPadding, "panelPadding must not be null");
  }

  /**
   * The default scale, sized for the {@link com.cryptroot.core.FontSize} faces: a 4 / 8 / 16
   * spacing ramp, symmetric control padding, and a 44-unit row height.
   */
  public static UiTheme standard() {
    return new UiTheme(4f, 8f, 16f, Insets.symmetric(12f, 6f), Insets.all(12f), 44f, 1f, 120f);
  }
}
