package com.cryptroot.performance;

import com.cryptroot.core.GameContext;
import com.cryptroot.performance.concurrent.WorkerPool;

/**
 * Concrete {@link GameContext} for the {@code performance} module's box-field showcase. Owns the
 * {@link WorkerPool} used by the parallel collision system (no static singleton — services hang off
 * the context), sized to the fixed arena passed in.
 */
public final class PerfDemoContext extends GameContext {

  private final WorkerPool workerPool;

  public PerfDemoContext(float worldWidth, float worldHeight) {
    super(worldWidth, worldHeight);
    workerPool = new WorkerPool();
  }

  public WorkerPool workerPool() {
    return workerPool;
  }

  @Override
  public void dispose() {
    super.dispose();
    workerPool.dispose();
  }
}
