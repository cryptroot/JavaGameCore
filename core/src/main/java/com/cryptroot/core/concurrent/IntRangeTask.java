package com.cryptroot.core.concurrent;

/**
 * A void unit of work over an int range, consumed by {@link WorkerPool#parallelFor}.
 *
 * @see WorkerPool
 */
@FunctionalInterface
public interface IntRangeTask {

  /** Performs the work for {@code [fromInclusive, toExclusive)}. */
  void run(int fromInclusive, int toExclusive);
}
