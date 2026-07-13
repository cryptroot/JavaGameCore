package com.cryptroot.core.physics;

import com.cryptroot.core.concurrent.WorkerPool;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Stateful system that tracks pairwise overlap between every {@link Collider}-carrying entity in a
 * {@link World} and fires {@link CollisionListener#onCollisionEnter()}/{@link
 * CollisionListener#onCollisionExit()} on the transitions — the framework form of Unity's {@code
 * OnTriggerEnter}/{@code OnTriggerExit}.
 *
 * <p>One instance per scene (it remembers the previous frame's touching pairs), mirroring {@link
 * com.cryptroot.core.render.system.HoverSystem}. Call {@link #reset()} when the scene is torn down.
 *
 * <p>Every {@link #update(World)} call re-scans {@link World#entities()} from scratch, so an entity
 * removed mid-overlap (even one queued for removal from inside the same frame's {@code
 * UpdateSystem} pass, and not yet flushed) is simply absent the next time it is no longer present —
 * its pairs are exited on that later {@link #update(World)} call with no special-cased cleanup
 * needed, exactly like {@code HoverSystem}'s live hit-test.
 *
 * <p><b>Broad-phase detection</b> is O(n²) pairwise across all {@link Collider} entities —
 * deliberately simple, matching this framework's "correctness first" style. Pass a {@link
 * WorkerPool} to {@link #CollisionSystem(WorkerPool)} to parallelize just that detection step once
 * the collider count reaches {@link #PARALLEL_THRESHOLD} (below that it always runs inline — thread
 * dispatch would outweigh the O(n²) work saved). Resolving the result — firing {@link
 * CollisionListener} enter/exit — always stays single-threaded and sequential regardless, since
 * listener callbacks are arbitrary game code that may mutate the world; only the read-only overlap
 * test ({@link Collider#overlaps}, which never mutates anything) runs concurrently. Behaviour is
 * identical either way, only wall-clock time differs. {@link
 * com.cryptroot.core.render.RenderPipeline} passes {@link
 * com.cryptroot.core.GameContext#workerPool()} automatically, so every consumer gets this for free
 * with no extra wiring — just attach {@link Collider} components as usual.
 */
public final class CollisionSystem {

  /**
   * Collider count at or above which {@link #CollisionSystem(WorkerPool)} switches detection from
   * the plain sequential loop to the {@link WorkerPool}-backed striped loop. Below this, detection
   * always runs inline on the calling thread — the same code path as the no-arg constructor —
   * because thread dispatch overhead would outweigh the O(n²) work saved at that scale.
   */
  static final int PARALLEL_THRESHOLD = 128;

  private final WorkerPool pool;
  private Set<PairKey> touching = new HashSet<>();

  /** Sequential detection only — no thread pool involved. */
  public CollisionSystem() {
    this.pool = null;
  }

  /**
   * Detection runs striped across {@code pool}'s threads once the collider count reaches {@link
   * #PARALLEL_THRESHOLD}; below that it runs inline, identical to {@link #CollisionSystem()}.
   */
  public CollisionSystem(WorkerPool pool) {
    this.pool = Objects.requireNonNull(pool, "pool must not be null");
  }

  /** Re-scans every {@link Collider}-carrying entity in {@code world} and fires transitions. */
  public void update(World world) {
    Objects.requireNonNull(world, "world must not be null");

    List<WorldEntity> colliders = new ArrayList<>();
    for (WorldEntity e : world.entities()) {
      if (e.has(Collider.class)) colliders.add(e);
    }

    Set<PairKey> current =
        (pool != null && colliders.size() >= PARALLEL_THRESHOLD)
            ? detectParallel(colliders, pool)
            : detectSequential(colliders);

    for (PairKey pair : current) {
      if (!touching.contains(pair)) notify(pair, true);
    }
    for (PairKey pair : touching) {
      if (!current.contains(pair)) notify(pair, false);
    }
    touching = current;
  }

  /**
   * Clears remembered overlap state (without firing exit signals). Call when the scene is torn
   * down.
   */
  public void reset() {
    touching = new HashSet<>();
  }

  private static Set<PairKey> detectSequential(List<WorldEntity> colliders) {
    Set<PairKey> current = new HashSet<>();
    for (int i = 0; i < colliders.size(); i++) {
      Collider a = colliders.get(i).get(Collider.class).get();
      for (int j = i + 1; j < colliders.size(); j++) {
        Collider b = colliders.get(j).get(Collider.class).get();
        if (a.overlaps(b)) {
          current.add(new PairKey(colliders.get(i), colliders.get(j)));
        }
      }
    }
    return current;
  }

  /**
   * Same result as {@link #detectSequential}, but the outer index is striped round-robin across
   * {@code pool}'s threads (not split into contiguous ranges), so every stripe gets an even mix of
   * "long" rows (low indices, which scan almost the whole array) and "short" rows (high indices,
   * which scan almost nothing) — a contiguous split would starve whichever thread gets the high
   * end.
   */
  private static Set<PairKey> detectParallel(List<WorldEntity> colliders, WorkerPool pool) {
    int n = colliders.size();
    Collider[] shapes = new Collider[n];
    for (int i = 0; i < n; i++) {
      shapes[i] = colliders.get(i).get(Collider.class).get();
    }

    int stripeCount = Math.min(pool.threads(), n);
    List<List<PairKey>> perStripe =
        pool.mapChunks(
            0,
            stripeCount,
            1,
            (loStripe, hiStripe) -> {
              List<PairKey> found = new ArrayList<>();
              for (int stripe = loStripe; stripe < hiStripe; stripe++) {
                for (int i = stripe; i < n; i += stripeCount) {
                  Collider a = shapes[i];
                  for (int j = i + 1; j < n; j++) {
                    if (a.overlaps(shapes[j])) {
                      found.add(new PairKey(colliders.get(i), colliders.get(j)));
                    }
                  }
                }
              }
              return found;
            });

    Set<PairKey> current = new HashSet<>();
    for (List<PairKey> stripeResult : perStripe) {
      current.addAll(stripeResult);
    }
    return current;
  }

  private static void notify(PairKey pair, boolean entered) {
    pair.a
        .get(CollisionListener.class)
        .ifPresent(l -> (entered ? l.onCollisionEnter() : l.onCollisionExit()).emit(pair.b));
    pair.b
        .get(CollisionListener.class)
        .ifPresent(l -> (entered ? l.onCollisionEnter() : l.onCollisionExit()).emit(pair.a));
  }

  /** Unordered pair of two entities, used as a hash-set key (order-independent equals/hashCode). */
  private static final class PairKey {
    final WorldEntity a;
    final WorldEntity b;

    PairKey(WorldEntity a, WorldEntity b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof PairKey other)) return false;
      return (a == other.a && b == other.b) || (a == other.b && b == other.a);
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(a) ^ System.identityHashCode(b);
    }
  }
}
