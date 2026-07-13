package com.cryptroot.core.world.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PathFollowerComponentTest {

  @Test
  void rejectsInvalidConstructorArguments() {
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    List<Vector2> waypoints = List.of(new Vector2(1f, 0f));

    assertThrows(NullPointerException.class, () -> new PathFollowerComponent(null, waypoints, 1f));
    assertThrows(NullPointerException.class, () -> new PathFollowerComponent(pos, null, 1f));
    assertThrows(
        IllegalArgumentException.class, () -> new PathFollowerComponent(pos, List.of(), 1f));
    assertThrows(
        IllegalArgumentException.class, () -> new PathFollowerComponent(pos, waypoints, 0f));
  }

  @Test
  void movesTowardCurrentWaypointEachUpdate() {
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    PathFollowerComponent follower =
        new PathFollowerComponent(pos, List.of(new Vector2(10f, 0f)), 2f);

    follower.update(1f); // moves 2 units toward (10,0)

    assertEquals(2f, pos.x(), 1e-5f);
    assertEquals(0f, pos.y(), 1e-5f);
    assertFalse(follower.isCompleted());
  }

  @Test
  void advancesThroughWaypointsInOrder() {
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    PathFollowerComponent follower =
        new PathFollowerComponent(pos, List.of(new Vector2(1f, 0f), new Vector2(1f, 5f)), 100f);

    follower.update(1f); // overshoots first waypoint in one big step

    assertEquals(1, follower.waypointIndex());
    assertFalse(follower.isCompleted());
  }

  @Test
  void firesOnCompletedExactlyOnceAtFinalWaypoint() {
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    PathFollowerComponent follower =
        new PathFollowerComponent(pos, List.of(new Vector2(1f, 0f), new Vector2(2f, 0f)), 100f);
    AtomicInteger completions = new AtomicInteger();
    follower.onCompleted().connect(completions::incrementAndGet);

    follower.update(1f); // reaches waypoint 0, advances
    follower.update(1f); // reaches waypoint 1 (final) — completes

    assertTrue(follower.isCompleted());
    assertEquals(1, completions.get());
    assertEquals(2f, pos.x(), 1e-5f);

    follower.update(1f); // no-op after completion
    assertEquals(1, completions.get());
    assertEquals(2f, pos.x(), 1e-5f);
  }

  @Test
  void singleWaypointCompletesInOneReachingStep() {
    DefaultPositionComponent pos = new DefaultPositionComponent(0f, 0f);
    PathFollowerComponent follower =
        new PathFollowerComponent(pos, List.of(new Vector2(1f, 0f)), 100f);

    follower.update(1f);

    assertTrue(follower.isCompleted());
    assertEquals(1f, pos.x(), 1e-5f);
  }
}
