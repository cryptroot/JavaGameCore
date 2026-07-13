package com.cryptroot.performance.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.event.Signal;
import com.cryptroot.core.physics.BoxCollider;
import com.cryptroot.core.physics.Collider;
import com.cryptroot.core.physics.CollisionListener;
import com.cryptroot.core.physics.CollisionSystem;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.component.DefaultPositionComponent;
import com.cryptroot.performance.concurrent.WorkerPool;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ParallelBroadPhaseTest {

  private static final float ARENA = 400f;
  private static final float BOX_SIZE = 8f;

  // ---------------------------------------------------------------------
  // Correctness: parallel detection matches sequential at scale
  // ---------------------------------------------------------------------

  @Test
  void detectMatchesSequentialAtScale() {
    World world = new World();
    List<WorldEntity> colliders =
        RandomColliderField.populate(world, 2000, ARENA, ARENA, BOX_SIZE, 42L);

    try (WorkerPool sequentialPool = new WorkerPool(1);
        WorkerPool parallelPool = new WorkerPool(8)) {
      Set<EntityPair> sequential = ParallelBroadPhase.detect(colliders, sequentialPool);
      Set<EntityPair> parallel = ParallelBroadPhase.detect(colliders, parallelPool);

      assertEquals(sequential, parallel);
      assertTrue(sequential.size() > 0, "expected at least one overlap at this density");
    }
  }

  @Test
  void detectReturnsEmptyForFewerThanTwoColliders() {
    try (WorkerPool pool = new WorkerPool(4)) {
      assertEquals(Set.of(), ParallelBroadPhase.detect(List.of(), pool));

      World single = new World();
      List<WorldEntity> oneEntity =
          RandomColliderField.populate(single, 1, ARENA, ARENA, BOX_SIZE, 1L);
      assertEquals(Set.of(), ParallelBroadPhase.detect(oneEntity, pool));
    }
  }

  @Test
  void detectRejectsNullArguments() {
    try (WorkerPool pool = new WorkerPool(1)) {
      assertThrows(NullPointerException.class, () -> ParallelBroadPhase.detect(null, pool));
      assertThrows(NullPointerException.class, () -> ParallelBroadPhase.detect(List.of(), null));
    }
  }

  // ---------------------------------------------------------------------
  // Parity with core.physics.CollisionSystem
  // ---------------------------------------------------------------------

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
    WorldEntity entity =
        new WorldEntity().with(Collider.class, collider).with(CollisionListener.class, listener);
    return world.add(entity);
  }

  @Test
  void parallelCollisionSystemMatchesCoreCollisionSystemEnterSet() {
    World world = new World();
    RecordingListener a = new RecordingListener();
    RecordingListener b = new RecordingListener();
    RecordingListener c = new RecordingListener(); // never overlaps
    boxAt(world, 0f, 0f, a);
    boxAt(world, 0.5f, 0f, b);
    boxAt(world, 100f, 100f, c);

    new CollisionSystem().update(world);
    Set<WorldEntity> coreEntered = Set.copyOf(a.entered);
    a.entered.clear();
    b.entered.clear();

    try (WorkerPool pool = new WorkerPool(4)) {
      new ParallelCollisionSystem(pool).update(world);
    }
    Set<WorldEntity> parallelEntered = Set.copyOf(a.entered);

    assertEquals(coreEntered, parallelEntered);
    assertTrue(c.entered.isEmpty());
  }

  // ---------------------------------------------------------------------
  // WorkerPool gate / fail-fast (exercised through the collision entry point)
  // ---------------------------------------------------------------------

  @Test
  void parallelForRunsEveryIndexBeforeReturning() {
    int n = 500;
    AtomicInteger[] touched = new AtomicInteger[n];
    for (int i = 0; i < n; i++) touched[i] = new AtomicInteger();

    try (WorkerPool pool = new WorkerPool()) {
      pool.parallelFor(
          0,
          n,
          8,
          (lo, hi) -> {
            for (int i = lo; i < hi; i++) touched[i].incrementAndGet();
          });
    }

    for (int i = 0; i < n; i++) {
      assertEquals(1, touched[i].get(), "index " + i + " should run exactly once");
    }
  }

  @Test
  void workerPoolRejectsInvalidArguments() {
    assertThrows(IllegalArgumentException.class, () -> new WorkerPool(0));
    try (WorkerPool pool = new WorkerPool(2)) {
      assertThrows(NullPointerException.class, () -> pool.parallelFor(0, 10, 1, null));
      assertThrows(
          IllegalArgumentException.class, () -> pool.parallelFor(10, 0, 1, (lo, hi) -> {}));
      assertThrows(
          IllegalArgumentException.class, () -> pool.parallelFor(0, 10, 0, (lo, hi) -> {}));
    }
  }
}
