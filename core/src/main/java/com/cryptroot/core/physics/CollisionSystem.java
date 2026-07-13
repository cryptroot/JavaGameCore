package com.cryptroot.core.physics;

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
 * <p>O(n²) pairwise broad-phase overlap test across all {@link Collider} entities — deliberately
 * simple, matching this framework's "correctness first" style; a spatial index would be a future
 * optimisation if entity counts ever demand it.
 */
public final class CollisionSystem {

  private Set<PairKey> touching = new HashSet<>();

  /** Re-scans every {@link Collider}-carrying entity in {@code world} and fires transitions. */
  public void update(World world) {
    Objects.requireNonNull(world, "world must not be null");

    List<WorldEntity> colliders = new ArrayList<>();
    for (WorldEntity e : world.entities()) {
      if (e.has(Collider.class)) colliders.add(e);
    }

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
