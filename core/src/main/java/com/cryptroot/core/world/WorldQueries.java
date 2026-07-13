package com.cryptroot.core.world;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Generic spatial queries over a {@link World}'s entities — the framework form of the hand-rolled
 * "find the nearest thing in range" scan every targeting mechanic (a tower, a spell, AI aggro) ends
 * up writing.
 */
public final class WorldQueries {

  private WorldQueries() {}

  /**
   * Returns the entity nearest to {@code (x, y)}, within {@code range}, that carries a {@code
   * componentType} component satisfying {@code filter} and a {@link PositionComponent} — or empty
   * if none qualify.
   *
   * <p>Ties (equal distance) resolve to whichever qualifying entity {@link World#entities()} visits
   * first.
   *
   * @param world the world to search
   * @param x world-space search origin X
   * @param y world-space search origin Y
   * @param range search radius in world units; must be positive
   * @param componentType the component type an entity must carry to be considered
   * @param filter additional predicate on that component (e.g. {@code Health::isAlive})
   */
  public static <T extends EntityComponent> Optional<WorldEntity> nearest(
      World world, float x, float y, float range, Class<T> componentType, Predicate<T> filter) {
    Objects.requireNonNull(world, "world must not be null");
    Objects.requireNonNull(componentType, "componentType must not be null");
    Objects.requireNonNull(filter, "filter must not be null");
    if (range <= 0f) {
      throw new IllegalArgumentException("range must be positive: " + range);
    }

    WorldEntity nearest = null;
    float nearestDistSq = range * range;
    for (WorldEntity e : world.entities()) {
      Optional<T> component = e.get(componentType);
      if (component.isEmpty() || !filter.test(component.get())) continue;
      Optional<PositionComponent> pos = e.get(PositionComponent.class);
      if (pos.isEmpty()) continue;

      float dx = pos.get().x() - x;
      float dy = pos.get().y() - y;
      float distSq = dx * dx + dy * dy;
      if (distSq <= nearestDistSq) {
        nearest = e;
        nearestDistSq = distSq;
      }
    }
    return Optional.ofNullable(nearest);
  }
}
