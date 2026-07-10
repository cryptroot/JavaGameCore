package com.cryptroot.demo;

import com.cryptroot.core.GameContext;

/**
 * Minimal concrete {@link GameContext} for the Cave TMX demo.
 *
 * <p>The world (and its {@code FitViewport}) is sized to the demo map's pixel dimensions so that
 * {@link CaveDemoScreen}'s world camera frames the whole map with no letterboxing.
 */
public final class DemoGameContext extends GameContext {

  /**
   * @param worldWidth world width in pixels (map columns × tile width)
   * @param worldHeight world height in pixels (map rows × tile height)
   */
  public DemoGameContext(float worldWidth, float worldHeight) {
    super(worldWidth, worldHeight);
  }
}
