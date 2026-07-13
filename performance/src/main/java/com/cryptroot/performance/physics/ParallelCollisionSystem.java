package com.cryptroot.performance.physics;

import com.cryptroot.core.physics.Collider;
import com.cryptroot.core.physics.CollisionListener;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.performance.concurrent.WorkerPool;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Experimental drop-in replacement for {@code core.physics.CollisionSystem} that parallelizes only
 * the broad-phase overlap <em>detection</em> (see {@link ParallelBroadPhase}) via a {@link
 * WorkerPool}. Firing {@link CollisionListener} enter/exit transitions afterwards stays
 * single-threaded and synchronous — those callbacks are arbitrary game code that may mutate the
 * world, so they must not run concurrently with each other. Behaviourally identical to {@code
 * CollisionSystem}: enter fires exactly once while overlapping, exit fires once when the overlap
 * ends, and an entity with a {@link Collider} but no listener still notifies the other side.
 *
 * <p>One instance per scene, same as {@code CollisionSystem} (it remembers the previous frame's
 * touching pairs). Call {@link #reset()} when the scene is torn down.
 *
 * <p>This is a deliberately experimental duplicate of {@code CollisionSystem}'s resolve step,
 * living in {@code performance} rather than {@code core} while the parallel approach is being
 * proven out — see the module's {@code CLAUDE.md} for the promotion plan.
 */
public final class ParallelCollisionSystem {

  private final WorkerPool pool;
  private Set<EntityPair> touching = new HashSet<>();

  public ParallelCollisionSystem(WorkerPool pool) {
    this.pool = Objects.requireNonNull(pool, "pool must not be null");
  }

  /** Re-scans every {@link Collider}-carrying entity in {@code world} and fires transitions. */
  public void update(World world) {
    Objects.requireNonNull(world, "world must not be null");

    List<WorldEntity> colliders = new ArrayList<>();
    for (WorldEntity e : world.entities()) {
      if (e.has(Collider.class)) colliders.add(e);
    }

    Set<EntityPair> current = ParallelBroadPhase.detect(colliders, pool);

    for (EntityPair pair : current) {
      if (!touching.contains(pair)) notify(pair, true);
    }
    for (EntityPair pair : touching) {
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

  private static void notify(EntityPair pair, boolean entered) {
    pair.a
        .get(CollisionListener.class)
        .ifPresent(l -> (entered ? l.onCollisionEnter() : l.onCollisionExit()).emit(pair.b));
    pair.b
        .get(CollisionListener.class)
        .ifPresent(l -> (entered ? l.onCollisionEnter() : l.onCollisionExit()).emit(pair.a));
  }
}
