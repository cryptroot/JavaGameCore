package com.cryptroot.performance.concurrent;

/** A unit of work over a contiguous integer range, run by {@link WorkerPool#parallelFor}. */
@FunctionalInterface
public interface IntRangeTask {

  /** Processes indices {@code [fromInclusive, toExclusive)}. */
  void run(int fromInclusive, int toExclusive);
}
