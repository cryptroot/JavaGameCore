package com.cryptroot.performance;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.performance.concurrent.WorkerPool;
import com.cryptroot.performance.physics.ParallelBroadPhase;
import com.cryptroot.performance.physics.RandomColliderField;
import java.util.List;
import java.util.Random;

/**
 * Headless (GL-free) benchmark: builds a large field of moving, colliding boxes and times
 * sequential vs. {@link ParallelBroadPhase} broad-phase detection at several entity counts.
 *
 * <p>Run with {@code mvn -pl performance -am exec:java
 * -Dexec.mainClass=com.cryptroot.performance.CollisionBenchmark}. This is a manual perf check, not
 * a correctness test (see {@code ParallelBroadPhaseTest} for that) — timings are
 * environment-dependent, so nothing here is asserted in CI.
 */
public final class CollisionBenchmark {

  private static final long SEED = 42L;
  private static final float ARENA_SIZE = 4000f;
  private static final float BOX_SIZE = 10f;
  private static final float SPEED = 60f; // world units/sec
  private static final float DELTA = 1f / 60f;
  private static final int WARMUP_FRAMES = 5;
  private static final int TIMED_FRAMES = 20;
  private static final int[] BOX_COUNTS = {1_000, 4_000, 16_000};

  private CollisionBenchmark() {}

  public static void main(String[] args) {
    int cores = Runtime.getRuntime().availableProcessors();
    System.out.println("Available processors: " + cores);
    System.out.printf(
        "%-8s %-22s %-22s %-10s%n", "boxes", "sequential ms/frame", "parallel ms/frame", "speedup");

    try (WorkerPool sequentialPool = new WorkerPool(1);
        WorkerPool parallelPool = new WorkerPool(cores)) {
      for (int count : BOX_COUNTS) {
        double sequentialMs = run(count, sequentialPool);
        double parallelMs = run(count, parallelPool);
        System.out.printf(
            "%-8d %-22.3f %-22.3f %-10.2fx%n",
            count, sequentialMs, parallelMs, sequentialMs / parallelMs);
      }
    }
  }

  private static double run(int boxCount, WorkerPool pool) {
    World world = new World();
    List<WorldEntity> entities =
        RandomColliderField.populate(world, boxCount, ARENA_SIZE, ARENA_SIZE, BOX_SIZE, SEED);
    Vector2[] velocities = randomVelocities(boxCount, SEED + 1);

    for (int frame = 0; frame < WARMUP_FRAMES; frame++) {
      step(entities, velocities);
      ParallelBroadPhase.detect(entities, pool);
    }

    long totalNanos = 0L;
    for (int frame = 0; frame < TIMED_FRAMES; frame++) {
      step(entities, velocities);
      long start = System.nanoTime();
      ParallelBroadPhase.detect(entities, pool);
      totalNanos += System.nanoTime() - start;
    }
    return (totalNanos / (double) TIMED_FRAMES) / 1_000_000.0;
  }

  private static void step(List<WorldEntity> entities, Vector2[] velocities) {
    for (int i = 0; i < entities.size(); i++) {
      PositionComponent pos = entities.get(i).get(PositionComponent.class).get();
      float nx = wrap(pos.x() + velocities[i].x * DELTA, ARENA_SIZE);
      float ny = wrap(pos.y() + velocities[i].y * DELTA, ARENA_SIZE);
      pos.moveTo(nx, ny);
    }
  }

  private static float wrap(float value, float max) {
    if (value < 0f) return value + max;
    if (value >= max) return value - max;
    return value;
  }

  private static Vector2[] randomVelocities(int count, long seed) {
    Random rng = new Random(seed);
    Vector2[] velocities = new Vector2[count];
    for (int i = 0; i < count; i++) {
      float angle = rng.nextFloat() * MathUtils.PI2;
      velocities[i] = new Vector2(MathUtils.cos(angle) * SPEED, MathUtils.sin(angle) * SPEED);
    }
    return velocities;
  }
}
