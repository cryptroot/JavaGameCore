package com.cryptroot.core;

/**
 * Symbolic font sizes. Use {@link AssetRegistry#font(FontSize)} to obtain the corresponding
 * pre-baked {@link com.badlogic.gdx.graphics.g2d.BitmapFont}.
 *
 * <p>Never call {@code setScale()} on fonts obtained from {@link AssetRegistry}; pick the
 * appropriate size here instead.
 */
public enum FontSize {
  /** Small hint / tooltip text (≈ 22 px). */
  HINT,
  /** Body / information text (≈ 34 px). */
  BODY,
  /** Menu item text (≈ 52 px). */
  MENU,
  /** Large title text (≈ 72 px). */
  TITLE
}
