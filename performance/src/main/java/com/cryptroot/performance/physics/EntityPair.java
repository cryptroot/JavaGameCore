package com.cryptroot.performance.physics;

import com.cryptroot.core.world.WorldEntity;
import java.util.Objects;

/**
 * Unordered pair of two entities — order-independent {@code equals}/{@code hashCode} so {@code (a,
 * b)} and {@code (b, a)} collide in a {@link java.util.Set}. The {@code performance} module's local
 * equivalent of {@code core.physics.CollisionSystem}'s private {@code PairKey}.
 */
final class EntityPair {

  final WorldEntity a;
  final WorldEntity b;

  EntityPair(WorldEntity a, WorldEntity b) {
    this.a = Objects.requireNonNull(a, "a must not be null");
    this.b = Objects.requireNonNull(b, "b must not be null");
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof EntityPair other)) return false;
    return (a == other.a && b == other.b) || (a == other.b && b == other.a);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(a) ^ System.identityHashCode(b);
  }
}
