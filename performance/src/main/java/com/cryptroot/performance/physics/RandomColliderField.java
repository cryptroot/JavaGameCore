package com.cryptroot.performance.physics;

import com.cryptroot.core.physics.BoxCollider;
import com.cryptroot.core.physics.Collider;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.component.DefaultPositionComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * GL-free builder for a {@link World} full of randomly-placed, square {@link BoxCollider} entities
 * — shared test data for {@code ParallelBroadPhaseTest} and {@code CollisionBenchmark}'s
 * large-scale "lots of boxes" showcase. Deliberately carries no render component: only {@link
 * PositionComponent} + {@link Collider}, since both consumers only exercise collision detection.
 */
public final class RandomColliderField {

  private RandomColliderField() {}

  /**
   * Adds {@code count} entities to {@code world} at uniformly-random positions within {@code [0,
   * arenaWidth - boxSize] x [0, arenaHeight - boxSize]}, each anchored by its own {@link
   * DefaultPositionComponent} and a {@code boxSize x boxSize} {@link BoxCollider}.
   *
   * @param seed fixed RNG seed so callers get a reproducible layout
   * @return the spawned entities, in spawn order
   */
  public static List<WorldEntity> populate(
      World world, int count, float arenaWidth, float arenaHeight, float boxSize, long seed) {
    Objects.requireNonNull(world, "world must not be null");
    if (count < 0) {
      throw new IllegalArgumentException("count must be >= 0, got " + count);
    }
    if (arenaWidth <= 0f || arenaHeight <= 0f) {
      throw new IllegalArgumentException(
          "arena size must be positive, got " + arenaWidth + "x" + arenaHeight);
    }
    if (boxSize <= 0f || boxSize > arenaWidth || boxSize > arenaHeight) {
      throw new IllegalArgumentException("boxSize must be positive and fit the arena");
    }

    Random rng = new Random(seed);
    List<WorldEntity> spawned = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      float x = rng.nextFloat() * (arenaWidth - boxSize);
      float y = rng.nextFloat() * (arenaHeight - boxSize);
      DefaultPositionComponent anchor = new DefaultPositionComponent(x, y);
      BoxCollider collider = new BoxCollider(anchor, 0f, 0f, boxSize, boxSize);
      WorldEntity entity =
          world.add(
              new WorldEntity()
                  .with(PositionComponent.class, anchor)
                  .with(Collider.class, collider));
      spawned.add(entity);
    }
    return spawned;
  }
}
