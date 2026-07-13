package com.cryptroot.core.world.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HomingProjectileComponentTest {

  private static WorldEntity spawnBound(
      World world, HomingProjectileComponent projectile, PositionComponent position) {
    WorldEntity self =
        world.add(
            new WorldEntity()
                .with(PositionComponent.class, position)
                .with(HomingProjectileComponent.class, projectile));
    projectile.bind(self);
    return self;
  }

  @Test
  void rejectsInvalidConstructorArguments() {
    World world = new World();
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    WorldEntity target =
        world.add(
            new WorldEntity().with(PositionComponent.class, new DefaultPositionComponent(1f, 0f)));

    assertThrows(
        NullPointerException.class,
        () -> new HomingProjectileComponent(null, pos, target, 1f, e -> true, e -> {}));
    assertThrows(
        NullPointerException.class,
        () -> new HomingProjectileComponent(world, pos, target, 1f, null, e -> {}));
    assertThrows(
        NullPointerException.class,
        () -> new HomingProjectileComponent(world, pos, target, 1f, e -> true, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new HomingProjectileComponent(world, pos, target, 0f, e -> true, e -> {}));
  }

  @Test
  void updateBeforeBindThrows() {
    World world = new World();
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    WorldEntity target =
        world.add(
            new WorldEntity().with(PositionComponent.class, new DefaultPositionComponent(1f, 0f)));
    HomingProjectileComponent projectile =
        new HomingProjectileComponent(world, pos, target, 1f, e -> true, e -> {});

    assertThrows(IllegalStateException.class, () -> projectile.update(0.1f));
  }

  @Test
  void movesTowardTargetEachFrame() {
    World world = new World();
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    WorldEntity target =
        world.add(
            new WorldEntity().with(PositionComponent.class, new DefaultPositionComponent(10f, 0f)));
    HomingProjectileComponent projectile =
        new HomingProjectileComponent(world, pos, target, 2f, e -> true, e -> {});
    spawnBound(world, projectile, pos);

    projectile.update(1f);

    assertEquals(2f, pos.x(), 1e-5f);
    assertFalse(projectile.isDone());
  }

  @Test
  void impactsExactlyOnceOnArrivalAndDespawns() {
    World world = new World();
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    WorldEntity target =
        world.add(
            new WorldEntity().with(PositionComponent.class, new DefaultPositionComponent(1f, 0f)));
    List<WorldEntity> impacted = new ArrayList<>();
    HomingProjectileComponent projectile =
        new HomingProjectileComponent(world, pos, target, 100f, e -> true, impacted::add);
    WorldEntity self = spawnBound(world, projectile, pos);

    projectile.update(1f); // overshoots the 1-unit gap in one big step

    assertTrue(projectile.isDone());
    assertEquals(List.of(target), impacted);
    assertTrue(world.entities().contains(self), "removal is deferred until flush");

    world.flushRemovals();
    assertFalse(world.entities().contains(self));

    projectile.update(1f); // no-op after done — no second impact
    assertEquals(List.of(target), impacted);
  }

  @Test
  void invalidTargetDespawnsWithoutImpact() {
    World world = new World();
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    WorldEntity target =
        world.add(
            new WorldEntity().with(PositionComponent.class, new DefaultPositionComponent(10f, 0f)));
    List<WorldEntity> impacted = new ArrayList<>();
    HomingProjectileComponent projectile =
        new HomingProjectileComponent(world, pos, target, 1f, e -> false, impacted::add);
    WorldEntity self = spawnBound(world, projectile, pos);

    projectile.update(1f);

    assertTrue(projectile.isDone());
    assertTrue(impacted.isEmpty());
    world.flushRemovals();
    assertFalse(world.entities().contains(self));
  }

  @Test
  void targetMissingPositionDespawnsWithoutImpact() {
    World world = new World();
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    WorldEntity target = world.add(new WorldEntity()); // no PositionComponent
    List<WorldEntity> impacted = new ArrayList<>();
    HomingProjectileComponent projectile =
        new HomingProjectileComponent(world, pos, target, 1f, e -> true, impacted::add);
    spawnBound(world, projectile, pos);

    projectile.update(1f);

    assertTrue(projectile.isDone());
    assertTrue(impacted.isEmpty());
  }
}
