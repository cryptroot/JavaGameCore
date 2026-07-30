package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Align;
import java.util.Objects;

/**
 * Shared UI layout arithmetic.
 *
 * <p>The alignment helpers deliberately take a plain {@code float capHeight} rather than a {@link
 * BitmapFont}: that keeps them pure float math, so they are unit-testable without a GL context, in
 * line with the convention that render/layout maths is extracted into {@code static} methods
 * (compare {@code WorldHealthBarComponent.barColor}).
 *
 * <p>Alignment is expressed with libGDX's {@link Align} bitmask constants, the same vocabulary
 * already used for text drawing, rather than a parallel enum.
 */
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

  /**
   * Returns the text baseline Y that vertically aligns a single line of the given cap height inside
   * the box {@code [boxY, boxY + boxHeight]}.
   *
   * <p>Recognised {@code align} flags, tested in order: {@link Align#bottom}, {@link Align#top},
   * anything else (including {@link Align#center}) centres.
   *
   * <ul>
   *   <li>{@code bottom} — glyphs sit on the box's bottom edge: {@code boxY}
   *   <li>{@code top} — cap height touches the box's top edge: {@code boxY + boxHeight - capHeight}
   *   <li>{@code center} — {@code boxY + (boxHeight + capHeight) / 2}
   * </ul>
   *
   * <p>Fail-soft by design: an unrecognised or combined mask centres rather than throwing, because
   * this is called from layout paths where a hard failure would be less useful than a sane default.
   *
   * @param boxY bottom edge of the box in world units
   * @param boxHeight height of the box
   * @param capHeight the font's cap height
   * @param align a libGDX {@link Align} bitmask
   */
  public static float baselineIn(float boxY, float boxHeight, float capHeight, int align) {
    if ((align & Align.bottom) != 0) {
      return boxY;
    }
    if ((align & Align.top) != 0) {
      return boxY + boxHeight - capHeight;
    }
    return boxY + (boxHeight + capHeight) / 2f;
  }

  /**
   * Returns the left X that horizontally aligns content of width {@code contentWidth} inside the
   * box {@code [boxX, boxX + boxWidth]}.
   *
   * <p>Recognised {@code align} flags, tested in order: {@link Align#left}, {@link Align#right},
   * anything else (including {@link Align#center}) centres.
   *
   * <p>Content wider than the box is not clamped — the returned X goes negative relative to the box
   * for centre and right alignment, which keeps overflow visually symmetric instead of silently
   * left-anchoring it. Clip the result if that matters.
   *
   * @param boxX left edge of the box in world units
   * @param boxWidth width of the box
   * @param contentWidth measured width of the content being placed
   * @param align a libGDX {@link Align} bitmask
   */
  public static float alignIn(float boxX, float boxWidth, float contentWidth, int align) {
    if ((align & Align.left) != 0) {
      return boxX;
    }
    if ((align & Align.right) != 0) {
      return boxX + boxWidth - contentWidth;
    }
    return boxX + (boxWidth - contentWidth) / 2f;
  }
}
