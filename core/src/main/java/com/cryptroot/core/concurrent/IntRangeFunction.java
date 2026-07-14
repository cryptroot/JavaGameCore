package com.cryptroot.core.concurrent;

/**
 * A value-producing computation over an int range, consumed by {@link WorkerPool#mapChunks}.
 *
 * @param <R> the result type
 * @see WorkerPool
 */
@FunctionalInterface
public interface IntRangeFunction<R> {

  /** Computes a result for {@code [fromInclusive, toExclusive)}. */
  R apply(int fromInclusive, int toExclusive);
}
