package com.cryptroot.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldRemovalTest {

  @Test
  void removeReturnsFalseForAbsentEntity() {
    World world = new World();
    assertFalse(world.remove(new WorldEntity()));
  }

  @Test
  void immediateRemoveFiresSignalAndDrops() {
    World world = new World();
    WorldEntity e = world.add(new WorldEntity());
    List<WorldEntity> fired = new ArrayList<>();
    world.onRemoved().connect(fired::add);

    assertTrue(world.remove(e));
    assertEquals(List.of(e), fired);
    assertEquals(0, world.entities().size());
    assertFalse(world.remove(e), "second remove is a no-op");
  }

  @Test
  void queuedRemovalAppliedOnFlush() {
    World world = new World();
    WorldEntity a = world.add(new WorldEntity());
    WorldEntity b = world.add(new WorldEntity());
    List<WorldEntity> fired = new ArrayList<>();
    world.onRemoved().connect(fired::add);

    world.queueRemove(a);
    assertEquals(2, world.entities().size(), "not removed until flush");
    assertTrue(fired.isEmpty());

    world.flushRemovals();
    assertEquals(List.of(a), fired);
    assertEquals(List.of(b), world.entities());
  }

  @Test
  void queueRemoveWhileIteratingLiveViewDoesNotThrow() {
    World world = new World();
    for (int i = 0; i < 5; i++) world.add(new WorldEntity());

    // Simulate a system iterating the live view and queuing removals.
    for (WorldEntity e : world.entities()) {
      world.queueRemove(e);
    }
    world.flushRemovals();
    assertEquals(0, world.entities().size());
  }

  @Test
  void duplicateQueueRemovesOnce() {
    World world = new World();
    WorldEntity e = world.add(new WorldEntity());
    List<WorldEntity> fired = new ArrayList<>();
    world.onRemoved().connect(fired::add);

    world.queueRemove(e);
    world.queueRemove(e);
    world.flushRemovals();

    assertEquals(1, fired.size(), "second queued removal collapses to a no-op");
  }

  @Test
  void clearDropsPendingAndFiresNoSignal() {
    World world = new World();
    WorldEntity e = world.add(new WorldEntity());
    List<WorldEntity> fired = new ArrayList<>();
    world.onRemoved().connect(fired::add);

    world.queueRemove(e);
    world.clear();
    world.flushRemovals();

    assertEquals(0, world.entities().size());
    assertTrue(fired.isEmpty(), "clear is bulk teardown, not per-entity despawn");
  }
}
