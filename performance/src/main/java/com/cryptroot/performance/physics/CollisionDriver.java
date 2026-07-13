package com.cryptroot.performance.physics;

import com.cryptroot.core.world.World;

/**
 * Common shape of "run one frame's collision step" — implemented via a method reference to either
 * {@code core.physics.CollisionSystem#update(World)} (the sequential baseline) or {@link
 * ParallelCollisionSystem#update(World)} (parallel detection), so a caller (the benchmark, the
 * visual demo) can hold a single toggleable reference without a wrapper class for either side.
 */
@FunctionalInterface
public interface CollisionDriver {

  void update(World world);
}
