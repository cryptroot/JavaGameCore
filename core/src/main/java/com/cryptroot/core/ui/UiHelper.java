package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.util.Objects;

/** Shared UI layout utility methods. */
public final class UiHelper {

  private UiHelper() {}

  /**
   * Returns the standard height for a single-line text bar: the font's cap height plus {@code
   * padding} on both the top and bottom edges.
   *
   * @param font the font whose cap height drives the measurement
   * @param padding vertical padding applied above and below the cap height
   * @return {@code font.getCapHeight() + padding * 2f}
   */
  public static float barHeight(BitmapFont font, float padding) {
    Objects.requireNonNull(font, "font must not be null");
    return font.getCapHeight() + padding * 2f;
  }
}
