package com.cryptroot.core.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.concurrent.WorkerPool;
import com.cryptroot.core.event.Signal;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.component.DefaultPositionComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CollisionSystemTest {

  private static final class RecordingListener implements CollisionListener {
    final List<WorldEntity> entered = new ArrayList<>();
    final List<WorldEntity> exited = new ArrayList<>();
    private final Signal<WorldEntity> onCollisionEnter = new Signal<>();
    private final Signal<WorldEntity> onCollisionExit = new Signal<>();

    RecordingListener() {
      onCollisionEnter.connect(entered::add);
      onCollisionExit.connect(exited::add);
    }

    @Override
    public Signal<WorldEntity> onCollisionEnter() {
      return onCollisionEnter;
    }

    @Override
    public Signal<WorldEntity> onCollisionExit() {
      return onCollisionExit;
    }
  }

  private static WorldEntity boxAt(World world, float x, float y, RecordingListener listener) {
    DefaultPositionComponent anchor = new DefaultPositionComponent(x, y);
    BoxCollider collider = new BoxCollider(anchor, 0f, 0f, 1f, 1f);
    WorldEntity entity = new WorldEntity().with(Collider.class, collider);
    if (listener != null) entity.with(CollisionListener.class, listener);
    return world.add(entity);
  }

  @Test
  void rejectsNullWorld() {
    assertThrows(NullPointerException.class, () -> new CollisionSystem().update(null));
  }

  @Test
  void firesEnterExactlyOnceWhileOverlapping() {
    World world = new World();
    RecordingListener a = new RecordingListener();
    RecordingListener b = new RecordingListener();
    WorldEntity ea = boxAt(world, 0f, 0f, a);
    WorldEntity eb = boxAt(world, 0.5f, 0f, b);
    CollisionSystem system = new CollisionSystem();

    system.update(world);
    system.update(world);
    system.update(world);

    assertEquals(List.of(eb), a.entered);
    assertEquals(List.of(ea), b.entered);
    assertTrue(a.exited.isEmpty());
    assertTrue(b.exited.isEmpty());
  }

  @Test
  void nonOverlappingEntitiesNeverNotify() {
    World world = new World();
    RecordingListener a = new RecordingListener();
    RecordingListener b = new RecordingListener();
    boxAt(world, 0f, 0f, a);
    boxAt(world, 100f, 100f, b);
    CollisionSystem system = new CollisionSystem();

    system.update(world);

    assertTrue(a.entered.isEmpty());
    assertTrue(b.entered.isEmpty());
  }

  @Test
  void firesExitOnceOverlapEnds() {
    World world = new World();
    RecordingListener a = new RecordingListener();
    RecordingListener b = new RecordingListener();
    WorldEntity ea = boxAt(world, 0f, 0f, a);
    DefaultPositionComponent movingAnchor = new DefaultPositionComponent(0.5f, 0f);
    WorldEntity eb =
        world.add(
            new WorldEntity()
                .with(Collider.class, new BoxCollider(movingAnchor, 0f, 0f, 1f, 1f))
                .with(CollisionListener.class, b));
    CollisionSystem system = new CollisionSystem();

    system.update(world); // overlapping
    assertEquals(List.of(eb), a.entered);

    movingAnchor.moveTo(100f, 100f); // move apart
    system.update(world);

    assertEquals(List.of(eb), a.entered, "enter must not fire twice");
    assertEquals(List.of(eb), a.exited);
    assertEquals(List.of(ea), b.exited);
  }

  @Test
  void colliderWithoutListenerStillNotifiesTheOtherSide() {
    World world = new World();
    RecordingListener a = new RecordingListener();
    WorldEntity ea = boxAt(world, 0f, 0f, a);
    WorldEntity eb = boxAt(world, 0.5f, 0f, null); // no listener on this side

    new CollisionSystem().update(world);

    assertEquals(List.of(eb), a.entered);
  }

  @Test
  void entityRemovedMidOverlapExitsOnNextUpdateWithoutSpecialCasing() {
    World world = new World();
    RecordingListener a = new RecordingListener();
    RecordingListener b = new RecordingListener();
    WorldEntity ea = boxAt(world, 0f, 0f, a);
    WorldEntity eb = boxAt(world, 0.5f, 0f, b);
    CollisionSystem system = new CollisionSystem();

    system.update(world);
    assertEquals(List.of(eb), a.entered);

    world.remove(eb);
    system.update(world);

    assertEquals(List.of(eb), a.exited);
  }

  @Test
  void resetClearsStateWithoutFiringExit() {
    World world = new World();
    RecordingListener a = new RecordingListener();
    RecordingListener b = new RecordingListener();
    boxAt(world, 0f, 0f, a);
    boxAt(world, 0.5f, 0f, b);
    CollisionSystem system = new CollisionSystem();
    system.update(world);

    system.reset();

    assertTrue(a.exited.isEmpty());
    assertTrue(b.exited.isEmpty());
  }

  // ---------------------------------------------------------------------
  // WorkerPool-backed (parallel) detection
  // ---------------------------------------------------------------------

  @Test
  void rejectsNullPool() {
    assertThrows(NullPointerException.class, () -> new CollisionSystem(null));
  }

  @Test
  void parallelConstructorBelowThresholdStillDetectsCorrectly() {
    // Far fewer colliders than PARALLEL_THRESHOLD - exercises the inline fallback inside the
    // pool-backed constructor, which must behave identically to the sequential constructor.
    World world = new World();
    RecordingListener a = new RecordingListener();
    RecordingListener b = new RecordingListener();
    WorldEntity ea = boxAt(world, 0f, 0f, a);
    WorldEntity eb = boxAt(world, 0.5f, 0f, b);

    try (WorkerPool pool = new WorkerPool(4)) {
      new CollisionSystem(pool).update(world);
    }

    assertEquals(List.of(eb), a.entered);
    assertEquals(List.of(ea), b.entered);
  }

  @Test
  void parallelDetectionMatchesSequentialAtScale() {
    World world = new World();
    int count = CollisionSystem.PARALLEL_THRESHOLD * 4;
    Random rng = new Random(7);
    for (int i = 0; i < count; i++) {
      float x = rng.nextFloat() * 200f;
      float y = rng.nextFloat() * 200f;
      DefaultPositionComponent anchor = new DefaultPositionComponent(x, y);
      BoxCollider collider = new BoxCollider(anchor, 0f, 0f, 5f, 5f);
      WorldEntity entity = new WorldEntity().with(Collider.class, collider);
      entity.with(CollisionListener.class, new RecordingListener());
      world.add(entity);
    }

    Set<WorldEntity> sequentialTouching = enteredEntities(world, new CollisionSystem());
    resetListeners(world);
    Set<WorldEntity> parallelTouching;
    try (WorkerPool pool = new WorkerPool(8)) {
      parallelTouching = enteredEntities(world, new CollisionSystem(pool));
    }

    assertEquals(sequentialTouching, parallelTouching);
    assertTrue(!sequentialTouching.isEmpty(), "expected at least one overlap at this density");
  }

  /** Runs one {@code update()} and returns every entity that fired at least one collision enter. */
  private static Set<WorldEntity> enteredEntities(World world, CollisionSystem system) {
    system.update(world);
    Set<WorldEntity> touched = new HashSet<>();
    for (WorldEntity e : world.entities()) {
      RecordingListener listener = (RecordingListener) e.get(CollisionListener.class).orElse(null);
      if (listener != null && !listener.entered.isEmpty()) touched.add(e);
    }
    return touched;
  }

  /** Clears every entity's recorded enter/exit lists so a second system can be measured cleanly. */
  private static void resetListeners(World world) {
    for (WorldEntity e : world.entities()) {
      RecordingListener listener = (RecordingListener) e.get(CollisionListener.class).orElse(null);
      if (listener != null) {
        listener.entered.clear();
        listener.exited.clear();
      }
    }
  }
}
