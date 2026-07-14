package com.cryptroot.core.concurrent;

import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * A small "thread manager" for CPU-bound, data-parallel work over an int range: a dedicated {@link
 * ForkJoinPool} (never the shared {@link ForkJoinPool#commonPool()}, so its lifecycle is owned and
 * disposed exactly like any other {@link Disposable} on a {@code GameContext}) plus a "fork many /
 * join when needed" primitive.
 *
 * <p>{@link #parallelFor} and {@link #mapChunks} split {@code [fromInclusive, toExclusive)} into up
 * to {@link #threads()} contiguous chunks (never smaller than {@code minChunkSize}) and submit one
 * task per chunk. {@link #mapChunks} does not block: it returns a {@link TaskGate} immediately, so
 * a caller can keep going and only pay for the "gate" — {@link TaskGate#get()} — once the results
 * are actually needed (e.g. after doing other work, or after handing the gate off to whatever code
 * needs the results). {@link #parallelFor} has no results to defer, so it still blocks until every
 * chunk has completed before returning, exactly as before. Ranges too small to be worth splitting
 * (or a pool sized to a single thread) run inline on the calling thread instead of paying
 * submission overhead — the result is identical either way, only the parallelism (and, for {@code
 * mapChunks}, whether {@link TaskGate#get()} actually blocks) differs.
 *
 * <p>Intended for read-only or embarrassingly-parallel work only (e.g. broad-phase overlap
 * detection, which only reads collider bounds). Callers remain responsible for keeping any
 * mutually-exclusive follow-up (mutating the world, firing listeners) single-threaded, after the
 * gate (see {@code CollisionSystem}).
 */
public final class WorkerPool implements Disposable, AutoCloseable {

  private final ForkJoinPool pool;
  private final int parallelism;

  /** Creates a pool sized to {@link Runtime#availableProcessors()}. */
  public WorkerPool() {
    this(Runtime.getRuntime().availableProcessors());
  }

  /** Creates a pool with a fixed parallelism (mainly for deterministic tests/benchmarks). */
  public WorkerPool(int parallelism) {
    if (parallelism < 1) {
      throw new IllegalArgumentException("parallelism must be >= 1, got " + parallelism);
    }
    this.parallelism = parallelism;
    this.pool = new ForkJoinPool(parallelism);
  }

  /** The configured parallelism (number of worker threads this pool was sized to). */
  public int threads() {
    return parallelism;
  }

  /**
   * Runs {@code task} once per chunk of {@code [fromInclusive, toExclusive)}, blocking until every
   * chunk has completed before returning.
   */
  public void parallelFor(int fromInclusive, int toExclusive, int minChunkSize, IntRangeTask task) {
    Objects.requireNonNull(task, "task must not be null");
    mapChunks(
            fromInclusive,
            toExclusive,
            minChunkSize,
            (lo, hi) -> {
              task.run(lo, hi);
              return null;
            })
        .get();
  }

  /**
   * Splits {@code [fromInclusive, toExclusive)} into chunks and submits one task per chunk without
   * blocking, returning a {@link TaskGate} that joins them (in range order) on {@link
   * TaskGate#get()}.
   *
   * @throws NullPointerException if {@code fn} is null
   * @throws IllegalArgumentException if {@code toExclusive < fromInclusive} or {@code minChunkSize
   *     < 1}
   */
  public <R> TaskGate<R> mapChunks(
      int fromInclusive, int toExclusive, int minChunkSize, IntRangeFunction<R> fn) {
    Objects.requireNonNull(fn, "fn must not be null");
    if (toExclusive < fromInclusive) {
      throw new IllegalArgumentException(
          "toExclusive (" + toExclusive + ") must be >= fromInclusive (" + fromInclusive + ")");
    }
    if (minChunkSize < 1) {
      throw new IllegalArgumentException("minChunkSize must be >= 1, got " + minChunkSize);
    }

    int length = toExclusive - fromInclusive;
    if (length == 0) return TaskGate.ofResults(List.of());

    int chunkCount = Math.max(1, Math.min(parallelism, length / minChunkSize));
    if (chunkCount == 1) {
      // Inline fast path: not worth (or not able to) split further — identical result, no
      // submission overhead, so the gate already has its result and never blocks.
      return TaskGate.ofResults(List.of(fn.apply(fromInclusive, toExclusive)));
    }

    int baseSize = length / chunkCount;
    int remainder = length % chunkCount;
    List<ForkJoinTask<R>> tasks = new ArrayList<>(chunkCount);
    int start = fromInclusive;
    for (int c = 0; c < chunkCount; c++) {
      int size = baseSize + (c < remainder ? 1 : 0);
      int chunkFrom = start;
      int chunkTo = start + size;
      start = chunkTo;
      tasks.add(pool.submit(() -> fn.apply(chunkFrom, chunkTo)));
    }

    return TaskGate.ofTasks(tasks);
  }

  @Override
  public void dispose() {
    pool.shutdown();
  }

  @Override
  public void close() {
    dispose();
  }
}
