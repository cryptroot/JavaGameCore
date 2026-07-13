package com.cryptroot.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.world.component.DefaultPositionComponent;
import com.cryptroot.core.world.component.HealthComponent;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorldQueriesTest {

  private static WorldEntity healthEntityAt(float x, float y, int hp) {
    HealthComponent health = new HealthComponent(hp == 0 ? 1 : hp);
    if (hp == 0) health.damage(1); // force dead (hp must start positive)
    return new WorldEntity()
        .with(PositionComponent.class, new DefaultPositionComponent(x, y))
        .with(HealthComponent.class, health);
  }

  @Test
  void returnsNearestQualifyingEntityWithinRange() {
    World world = new World();
    WorldEntity near = world.add(healthEntityAt(2f, 0f, 10));
    world.add(healthEntityAt(8f, 0f, 10));

    Optional<WorldEntity> found =
        WorldQueries.nearest(world, 0f, 0f, 20f, HealthComponent.class, HealthComponent::isAlive);

    assertTrue(found.isPresent());
    assertEquals(near, found.get());
  }

  @Test
  void ignoresEntitiesOutsideRange() {
    World world = new World();
    world.add(healthEntityAt(100f, 0f, 10));

    Optional<WorldEntity> found =
        WorldQueries.nearest(world, 0f, 0f, 5f, HealthComponent.class, HealthComponent::isAlive);

    assertTrue(found.isEmpty());
  }

  @Test
  void ignoresEntitiesFailingFilter() {
    World world = new World();
    world.add(healthEntityAt(1f, 0f, 0)); // dead — filtered out
    WorldEntity alive = world.add(healthEntityAt(4f, 0f, 10));

    Optional<WorldEntity> found =
        WorldQueries.nearest(world, 0f, 0f, 20f, HealthComponent.class, HealthComponent::isAlive);

    assertEquals(alive, found.get());
  }

  @Test
  void ignoresEntitiesMissingTheComponentOrPosition() {
    World world = new World();
    world.add(new WorldEntity()); // neither component
    world.add(
        new WorldEntity().with(HealthComponent.class, new HealthComponent(10))); // no position

    Optional<WorldEntity> found =
        WorldQueries.nearest(world, 0f, 0f, 20f, HealthComponent.class, HealthComponent::isAlive);

    assertTrue(found.isEmpty());
  }

  @Test
  void rejectsNonPositiveRange() {
    World world = new World();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorldQueries.nearest(
                world, 0f, 0f, 0f, HealthComponent.class, HealthComponent::isAlive));
  }

  @Test
  void rejectsNullArguments() {
    World world = new World();
    assertThrows(
        NullPointerException.class,
        () -> WorldQueries.nearest(null, 0f, 0f, 1f, HealthComponent.class, h -> true));
    assertThrows(
        NullPointerException.class, () -> WorldQueries.nearest(world, 0f, 0f, 1f, null, h -> true));
  }
}
