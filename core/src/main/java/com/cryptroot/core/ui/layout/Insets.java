package com.cryptroot.core.ui.layout;

/**
 * Immutable edge insets in world units, used for container padding and widget content margins.
 *
 * <p>Component order follows the toolkit's Y-up, bottom-left-origin rectangle convention ({@code x,
 * y, width, height} → left, bottom): {@code left}, {@code bottom}, {@code right}, {@code top}.
 *
 * <p>Fails fast on any negative component — a negative inset would silently grow a container's
 * content area beyond its own frame, which reads as a layout bug rather than an intent.
 *
 * @param left inset from the left edge
 * @param bottom inset from the bottom edge
 * @param right inset from the right edge
 * @param top inset from the top edge
 */
public record Insets(float left, float bottom, float right, float top) {

  /** Zero on every edge. */
  public static final Insets NONE = new Insets(0f, 0f, 0f, 0f);

  public Insets {
    if (left < 0f || bottom < 0f || right < 0f || top < 0f) {
      throw new IllegalArgumentException(
          "insets must not be negative, got left="
              + left
              + " bottom="
              + bottom
              + " right="
              + right
              + " top="
              + top);
    }
  }

  /** The same inset on all four edges. */
  public static Insets all(float value) {
    return new Insets(value, value, value, value);
  }

  /**
   * {@code horizontal} on the left and right edges, {@code vertical} on the bottom and top — the
   * common case for control padding.
   */
  public static Insets symmetric(float horizontal, float vertical) {
    return new Insets(horizontal, vertical, horizontal, vertical);
  }

  /** Total width consumed: {@code left + right}. */
  public float horizontal() {
    return left + right;
  }

  /** Total height consumed: {@code bottom + top}. */
  public float vertical() {
    return bottom + top;
  }
}
