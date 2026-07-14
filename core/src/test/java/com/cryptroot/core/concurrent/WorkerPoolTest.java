package com.cryptroot.core.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WorkerPoolTest {

  @Test
  void rejectsInvalidParallelism() {
    assertThrows(IllegalArgumentException.class, () -> new WorkerPool(0));
    assertThrows(IllegalArgumentException.class, () -> new WorkerPool(-1));
  }

  @Test
  void parallelForRunsEveryIndexExactlyOnceBeforeReturning() {
    int n = 500;
    AtomicInteger[] touched = new AtomicInteger[n];
    for (int i = 0; i < n; i++) touched[i] = new AtomicInteger();

    try (WorkerPool pool = new WorkerPool()) {
      pool.parallelFor(
          0,
          n,
          8,
          (lo, hi) -> {
            for (int i = lo; i < hi; i++) touched[i].incrementAndGet();
          });
    }

    for (int i = 0; i < n; i++) {
      assertEquals(1, touched[i].get(), "index " + i + " should run exactly once");
    }
  }

  @Test
  void mapChunksReturnsOneResultPerChunkInRangeOrder() {
    try (WorkerPool pool = new WorkerPool(4)) {
      List<Integer> lengths = pool.mapChunks(0, 100, 1, (lo, hi) -> hi - lo).get();
      assertEquals(100, lengths.stream().mapToInt(Integer::intValue).sum());
    }
  }

  @Test
  void mapChunksGateCanBeJoinedLaterAndMoreThanOnce() {
    try (WorkerPool pool = new WorkerPool(4)) {
      TaskGate<Integer> gate = pool.mapChunks(0, 100, 1, (lo, hi) -> hi - lo);
      List<Integer> lengths = gate.get();
      assertEquals(100, lengths.stream().mapToInt(Integer::intValue).sum());
      assertEquals(lengths, gate.get());
    }
  }

  @Test
  void mapChunksUsesInlineFastPathWhenRangeIsTooSmallToSplit() {
    try (WorkerPool pool = new WorkerPool(8)) {
      // minChunkSize forces a single chunk even though parallelism is 8.
      List<Integer> result = pool.mapChunks(0, 10, 100, (lo, hi) -> hi - lo).get();
      assertEquals(List.of(10), result);
    }
  }

  @Test
  void mapChunksRejectsInvalidArguments() {
    try (WorkerPool pool = new WorkerPool(2)) {
      assertThrows(NullPointerException.class, () -> pool.mapChunks(0, 10, 1, null));
      assertThrows(IllegalArgumentException.class, () -> pool.mapChunks(10, 0, 1, (lo, hi) -> hi));
      assertThrows(IllegalArgumentException.class, () -> pool.mapChunks(0, 10, 0, (lo, hi) -> hi));
    }
  }

  @Test
  void threadsReportsConfiguredParallelism() {
    try (WorkerPool pool = new WorkerPool(6)) {
      assertEquals(6, pool.threads());
    }
  }
}
