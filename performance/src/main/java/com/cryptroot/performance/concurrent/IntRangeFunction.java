package com.cryptroot.performance.concurrent;

/**
 * A computation over a contiguous integer range that produces a result, run by {@link
 * WorkerPool#mapChunks}.
 *
 * @param <R> the per-chunk result type
 */
@FunctionalInterface
public interface IntRangeFunction<R> {

  /** Processes indices {@code [fromInclusive, toExclusive)} and returns this chunk's result. */
  R apply(int fromInclusive, int toExclusive);
}
