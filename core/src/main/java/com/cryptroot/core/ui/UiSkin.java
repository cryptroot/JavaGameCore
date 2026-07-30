package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import java.util.Objects;

/**
 * Immutable bundle of the shared rendering resources and metrics a UI widget draws itself with: two
 * nine-patch slices, a font, and a {@link UiTheme} spacing scale.
 *
 * <p>Obtain one from {@link com.cryptroot.core.AssetRegistry#defaultSkin()}, or {@link
 * com.cryptroot.core.AssetRegistry#skin(com.cryptroot.core.FontSize)} for a specific font size:
 *
 * <pre>{@code
 * UiSkin rowSkin = context.assets().skin(FontSize.HINT);
 * Button row = new Button(rowSkin, "R1 [vacant]");
 * }</pre>
 *
 * <p>Use {@link #withFont(BitmapFont)} / {@link #withTheme(UiTheme)} to derive a variant rather
 * than re-listing every component.
 *
 * <p>The record holds no native LibGDX resources of its own and does not need to be disposed — the
 * font and slices are owned by {@link com.cryptroot.core.AssetRegistry}.
 *
 * @param normalSlice border drawn in the resting and hovered states
 * @param selectedSlice border drawn while pressed or selected
 * @param font the face all text in the widget is drawn with
 * @param theme the spacing / metric scale, see {@link UiTheme}
 */
public record UiSkin(
    NinePatch normalSlice, NinePatch selectedSlice, BitmapFont font, UiTheme theme) {

  public UiSkin {
    Objects.requireNonNull(normalSlice, "normalSlice must not be null");
    Objects.requireNonNull(selectedSlice, "selectedSlice must not be null");
    Objects.requireNonNull(font, "font must not be null");
    Objects.requireNonNull(theme, "theme must not be null");
  }

  /** Convenience constructor applying {@link UiTheme#standard()}. */
  public UiSkin(NinePatch normalSlice, NinePatch selectedSlice, BitmapFont font) {
    this(normalSlice, selectedSlice, font, UiTheme.standard());
  }

  /** Returns a copy of this skin drawing text with {@code other} instead. */
  public UiSkin withFont(BitmapFont other) {
    Objects.requireNonNull(other, "other must not be null");
    return new UiSkin(normalSlice, selectedSlice, other, theme);
  }

  /** Returns a copy of this skin using {@code other}'s spacing scale instead. */
  public UiSkin withTheme(UiTheme other) {
    Objects.requireNonNull(other, "other must not be null");
    return new UiSkin(normalSlice, selectedSlice, font, other);
  }
}
