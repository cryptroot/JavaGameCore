package com.cryptroot.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorldAdditionTest {

  @Test
  void immediateAddIsVisibleRightAway() {
    World world = new World();
    WorldEntity e = world.add(new WorldEntity());
    assertEquals(List.of(e), world.entities());
  }

  @Test
  void queuedAdditionNotVisibleUntilFlush() {
    World world = new World();
    WorldEntity e = new WorldEntity();

    WorldEntity returned = world.queueAdd(e);
    assertSame(e, returned, "queueAdd returns the same entity for fluent post-wiring");
    assertTrue(world.entities().isEmpty(), "not visible until flush");

    world.flushAdditions();
    assertEquals(List.of(e), world.entities());
  }

  @Test
  void queueAddWhileIteratingLiveViewDoesNotThrow() {
    World world = new World();
    for (int i = 0; i < 5; i++) world.add(new WorldEntity());

    // Simulate a system iterating the live view and queuing new entities (e.g. a spawner).
    List<WorldEntity> spawned = new ArrayList<>();
    for (WorldEntity e : world.entities()) {
      spawned.add(world.queueAdd(new WorldEntity()));
    }
    assertEquals(5, world.entities().size(), "not visible until flush");

    world.flushAdditions();
    assertEquals(10, world.entities().size());
    for (WorldEntity e : spawned) {
      assertTrue(world.entities().contains(e));
    }
  }

  @Test
  void clearDiscardsPendingAdditions() {
    World world = new World();
    world.queueAdd(new WorldEntity());

    world.clear();
    world.flushAdditions();

    assertTrue(world.entities().isEmpty());
  }
}
