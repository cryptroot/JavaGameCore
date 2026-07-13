package com.cryptroot.performance.physics;

import com.cryptroot.core.physics.Collider;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.performance.concurrent.WorkerPool;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Parallel broad-phase overlap detection over a fixed snapshot of {@link Collider}-carrying
 * entities.
 *
 * <p><b>Why this is safe to parallelize:</b> {@link Collider#overlaps} only reads each entity's
 * current bounds ({@code core.physics.BoxCollider#bounds} reads a live {@code PositionComponent}
 * but never mutates it), so concurrently overlap-testing disjoint pairs never races — nothing in
 * {@code colliders} is mutated while this runs. This is deliberately scoped to <em>detection</em>
 * only: resolving the result (firing {@code CollisionListener} enter/exit) must stay
 * single-threaded, since listener callbacks are arbitrary game code that may mutate the world — see
 * {@link ParallelCollisionSystem#update}.
 *
 * <p><b>Load balancing:</b> the classic triangular double loop ({@code for i}, {@code for j =
 * i+1..n}) does unequal work per {@code i} — index {@code 0} scans almost the whole array, the last
 * index scans nothing. A naive contiguous split of {@code i} across threads would starve whichever
 * thread gets the high end. Instead the outer index is striped round-robin across a fixed number of
 * stripes (one per {@link WorkerPool#threads()}, capped at the entity count), so every stripe gets
 * an even mix of "long" and "short" rows.
 *
 * <p>The returned {@link Set} is order-independent and content-identical regardless of thread count
 * or how the pool happens to chunk the stripe range.
 */
public final class ParallelBroadPhase {

  private ParallelBroadPhase() {}

  /**
   * @param colliders a fixed snapshot of entities; every entity must carry a {@link Collider} (the
   *     same precondition {@code core.physics.CollisionSystem} enforces internally)
   * @param pool the worker pool to distribute stripes across
   */
  public static Set<EntityPair> detect(List<WorldEntity> colliders, WorkerPool pool) {
    Objects.requireNonNull(colliders, "colliders must not be null");
    Objects.requireNonNull(pool, "pool must not be null");

    int n = colliders.size();
    if (n < 2) return Set.of();

    Collider[] shapes = new Collider[n];
    for (int i = 0; i < n; i++) {
      shapes[i] = colliders.get(i).get(Collider.class).get();
    }

    int stripeCount = Math.min(pool.threads(), n);
    List<List<EntityPair>> perChunk =
        pool.mapChunks(
            0,
            stripeCount,
            1,
            (loStripe, hiStripe) -> {
              List<EntityPair> found = new ArrayList<>();
              for (int stripe = loStripe; stripe < hiStripe; stripe++) {
                detectStripe(colliders, shapes, stripe, stripeCount, found);
              }
              return found;
            });

    Set<EntityPair> merged = new HashSet<>();
    for (List<EntityPair> chunk : perChunk) {
      merged.addAll(chunk);
    }
    return merged;
  }

  private static void detectStripe(
      List<WorldEntity> colliders,
      Collider[] shapes,
      int stripe,
      int stripeCount,
      List<EntityPair> out) {
    int n = shapes.length;
    for (int i = stripe; i < n; i += stripeCount) {
      Collider a = shapes[i];
      for (int j = i + 1; j < n; j++) {
        if (a.overlaps(shapes[j])) {
          out.add(new EntityPair(colliders.get(i), colliders.get(j)));
        }
      }
    }
  }
}
