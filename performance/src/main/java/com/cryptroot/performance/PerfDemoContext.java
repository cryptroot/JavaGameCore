package com.cryptroot.performance;

import com.cryptroot.core.GameContext;

/**
 * Concrete {@link GameContext} for the {@code performance} module's box-field showcase. Carries no
 * extra state of its own — {@link GameContext#workerPool()} (used by the parallel {@code
 * CollisionSystem} instance in {@code BoxFieldScreen}) is already provided by the base class.
 */
public final class PerfDemoContext extends GameContext {

  public PerfDemoContext(float worldWidth, float worldHeight) {
    super(worldWidth, worldHeight);
  }
}
