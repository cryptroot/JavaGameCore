package com.cryptroot.performance;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.concurrent.WorkerPool;
import com.cryptroot.core.physics.CollisionSystem;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.performance.physics.RandomColliderField;
import java.util.List;
import java.util.Random;

/**
 * Headless (GL-free) benchmark: builds a large field of moving, colliding boxes and times {@link
 * CollisionSystem} built with its sequential no-arg constructor vs. its {@link
 * com.cryptroot.core.concurrent.WorkerPool}-backed constructor, at several entity counts.
 *
 * <p>Run with {@code mvn -pl performance -am exec:java
 * -Dexec.mainClass=com.cryptroot.performance.CollisionBenchmark}. This is a manual perf check, not
 * a correctness test (see {@code core.physics.CollisionSystemTest} for that) — timings are
 * environment-dependent, so nothing here is asserted in CI. The smallest box count is deliberately
 * below {@code CollisionSystem}'s internal parallel threshold, to demonstrate that the pool-backed
 * constructor costs nothing extra at a scale too small to benefit.
 */
public final class CollisionBenchmark {

  private static final long SEED = 42L;
  private static final float ARENA_SIZE = 4000f;
  private static final float BOX_SIZE = 10f;
  private static final float SPEED = 60f; // world units/sec
  private static final float DELTA = 1f / 60f;
  private static final int WARMUP_FRAMES = 5;
  private static final int TIMED_FRAMES = 20;
  private static final int[] BOX_COUNTS = {100, 1_000, 4_000, 16_000};

  private CollisionBenchmark() {}

  public static void main(String[] args) {
    int cores = Runtime.getRuntime().availableProcessors();
    System.out.println("Available processors: " + cores);
    System.out.printf(
        "%-8s %-22s %-22s %-10s%n", "boxes", "sequential ms/frame", "parallel ms/frame", "speedup");

    try (WorkerPool parallelPool = new WorkerPool(cores)) {
      for (int count : BOX_COUNTS) {
        double sequentialMs = run(count, new CollisionSystem());
        double parallelMs = run(count, new CollisionSystem(parallelPool));
        System.out.printf(
            "%-8d %-22.3f %-22.3f %-10.2fx%n",
            count, sequentialMs, parallelMs, sequentialMs / parallelMs);
      }
    }
  }

  private static double run(int boxCount, CollisionSystem system) {
    World world = new World();
    List<WorldEntity> entities =
        RandomColliderField.populate(world, boxCount, ARENA_SIZE, ARENA_SIZE, BOX_SIZE, SEED);
    Vector2[] velocities = randomVelocities(boxCount, SEED + 1);

    for (int frame = 0; frame < WARMUP_FRAMES; frame++) {
      step(entities, velocities);
      system.update(world);
    }

    long totalNanos = 0L;
    for (int frame = 0; frame < TIMED_FRAMES; frame++) {
      step(entities, velocities);
      long start = System.nanoTime();
      system.update(world);
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
