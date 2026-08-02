package com.cryptroot.core.uitest.example;

import com.cryptroot.core.GameContext;

/**
 * Minimal {@link GameContext} for the harness's own example test — services only, no game state.
 *
 * <p>{@code core} cannot depend on a game module, so the example screen it exercises is built here
 * out of nothing but {@code core} widgets.
 */
final class PlaygroundContext extends GameContext {

  /** Arbitrary virtual world size; the widgets are laid out from the viewport, not from these. */
  static final float WORLD_WIDTH = 1600f;

  static final float WORLD_HEIGHT = 1000f;

  PlaygroundContext() {
    super(WORLD_WIDTH, WORLD_HEIGHT);
  }
}
