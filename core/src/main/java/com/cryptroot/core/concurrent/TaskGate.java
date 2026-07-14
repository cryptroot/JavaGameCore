package com.cryptroot.core.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;

/**
 * A handle to the chunks submitted by {@link WorkerPool#mapChunks}. Submitting never blocks the
 * calling thread; {@link #get()} is the only "gate" — it blocks until every outstanding chunk has
 * completed (a no-op if they already have) and returns one result per chunk in range order.
 *
 * <p>Returning this instead of blocking inside {@code mapChunks} lets a caller kick off work early,
 * go do unrelated CPU work of its own (or hand the gate to whatever code actually needs the
 * results), and only pay for the wait once {@link #get()} is called. Compare {@link
 * WorkerPool#parallelFor}, which has no results to defer and therefore still blocks before
 * returning.
 *
 * @param <R> the per-chunk result type
 */
public final class TaskGate<R> {

  private final List<R> results;
  private final List<ForkJoinTask<R>> tasks;

  private TaskGate(List<R> results, List<ForkJoinTask<R>> tasks) {
    this.results = results;
    this.tasks = tasks;
  }

  /** Wraps already-computed results (the inline fast path) — {@link #get()} never blocks. */
  static <R> TaskGate<R> ofResults(List<R> results) {
    return new TaskGate<>(results, null);
  }

  /** Wraps in-flight tasks — {@link #get()} joins each one, in range order. */
  static <R> TaskGate<R> ofTasks(List<ForkJoinTask<R>> tasks) {
    return new TaskGate<>(null, tasks);
  }

  /**
   * Blocks until every submitted chunk has completed, then returns one result per chunk in range
   * order. Safe to call more than once.
   */
  public List<R> get() {
    if (results != null) {
      return results;
    }
    List<R> joined = new ArrayList<>(tasks.size());
    for (ForkJoinTask<R> t : tasks) {
      joined.add(t.join());
    }
    return joined;
  }
}
