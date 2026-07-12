package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import java.util.Objects;

/**
 * Immutable bundle of shared rendering resources used by UI widgets.
 *
 * <p>Pass a {@code UiSkin} to widget constructors instead of {@link com.cryptroot.core.MyDemoGame
 * MyJourneyGame} directly. Obtain a default instance from {@link
 * com.cryptroot.core.MyDemoGame#defaultSkin() MyJourneyGame.defaultSkin()}. When a widget needs a
 * different font size, build a variant skin inline:
 *
 * <pre>{@code
 * UiSkin menuSkin = new UiSkin(
 *     game.defaultSkin().normalSlice(),
 *     game.defaultSkin().selectedSlice(),
 *     game.getFontMenu());
 * }</pre>
 *
 * <p>The record holds no native LibGDX resources of its own and does not need to be disposed.
 */
public record UiSkin(NinePatch normalSlice, NinePatch selectedSlice, BitmapFont font) {
  public UiSkin {
    Objects.requireNonNull(normalSlice, "normalSlice must not be null");
    Objects.requireNonNull(selectedSlice, "selectedSlice must not be null");
    Objects.requireNonNull(font, "font must not be null");
  }
}
