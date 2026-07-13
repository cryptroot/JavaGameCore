package com.cryptroot.performance.concurrent;

import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * Experimental thread manager for the {@code performance} module's parallel showcases: a dedicated
 * {@link ForkJoinPool} (never the shared {@link ForkJoinPool#commonPool()}, so its lifecycle is
 * owned and disposed exactly like any other {@link Disposable} on a {@code GameContext}) plus a
 * blocking "fork many / join all" primitive.
 *
 * <p>{@link #parallelFor} and {@link #mapChunks} split {@code [fromInclusive, toExclusive)} into up
 * to {@link #threads()} contiguous chunks (never smaller than {@code minChunkSize}), submit one
 * task per chunk, then <em>block</em> until every chunk has completed before returning — the "gate"
 * a caller waits on before touching results that require single-threaded, mutually exclusive
 * follow-up (e.g. firing collision enter/exit signals; see {@code ParallelCollisionSystem}). Ranges
 * too small to be worth splitting (or a pool sized to a single thread) run inline on the calling
 * thread instead of paying submission overhead — the result is identical either way, only the
 * parallelism differs.
 *
 * <p>This class deliberately lives in {@code performance} as an experimental prototype rather than
 * {@code core.concurrent}; promote it once a real game needs general-purpose parallel work.
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
        });
  }

  /**
   * Splits {@code [fromInclusive, toExclusive)} into chunks and applies {@code fn} to each,
   * blocking until every chunk has completed, then returns one result per chunk in range order.
   *
   * @throws NullPointerException if {@code fn} is null
   * @throws IllegalArgumentException if {@code toExclusive < fromInclusive} or {@code minChunkSize
   *     < 1}
   */
  public <R> List<R> mapChunks(
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
    if (length == 0) return List.of();

    int chunkCount = Math.max(1, Math.min(parallelism, length / minChunkSize));
    if (chunkCount == 1) {
      // Inline fast path: not worth (or not able to) split further — identical result, no
      // submission overhead.
      return List.of(fn.apply(fromInclusive, toExclusive));
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

    // Gate: block until every chunk has finished before the caller proceeds.
    List<R> results = new ArrayList<>(chunkCount);
    for (ForkJoinTask<R> t : tasks) {
      results.add(t.join());
    }
    return results;
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
