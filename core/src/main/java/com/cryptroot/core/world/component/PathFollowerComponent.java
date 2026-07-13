package com.cryptroot.core.world.component;

import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.event.Signal0;
import com.cryptroot.core.time.Motion;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.UpdateComponent;
import java.util.List;
import java.util.Objects;

/**
 * Moves a {@link PositionComponent} along a fixed polyline of world-space waypoints at a constant
 * speed — the framework form of the hand-rolled "follow this path" logic every waypoint-driven
 * mover (a tower-defense enemy, an RTS unit, a patrolling NPC) ends up writing.
 *
 * <p>Advances through the waypoint list in order via {@link Motion#moveTowards}; fires {@link
 * #onCompleted()} exactly once, when the final waypoint is reached, and then goes idle (further
 * {@link #update} calls are no-ops).
 *
 * <p>Does not replan or recompute the route if the world changes after construction — that is a
 * deliberate scope cut, matching the behaviour of the tower-defense demo this was extracted from. A
 * game that needs to react to a changed {@link com.cryptroot.core.path.Board} mid-flight should
 * construct a new instance (or bind a new one) once it detects the change.
 */
public final class PathFollowerComponent implements UpdateComponent {

  private final PositionComponent target;
  private final List<Vector2> waypoints;
  private final float speed;
  private final Signal0 onCompleted = new Signal0();
  private final Vector2 scratch = new Vector2();

  private int waypointIndex;
  private boolean completed;

  /**
   * @param target the position this component moves every frame
   * @param waypoints the polyline to follow, in order; copied defensively, must not be empty
   * @param speed movement speed in world units per second; must be positive
   */
  public PathFollowerComponent(PositionComponent target, List<Vector2> waypoints, float speed) {
    this.target = Objects.requireNonNull(target, "target must not be null");
    Objects.requireNonNull(waypoints, "waypoints must not be null");
    if (waypoints.isEmpty()) {
      throw new IllegalArgumentException("waypoints must not be empty");
    }
    if (speed <= 0f) {
      throw new IllegalArgumentException("speed must be positive: " + speed);
    }
    this.waypoints = List.copyOf(waypoints);
    this.speed = speed;
  }

  @Override
  public void update(float delta) {
    if (completed) return;

    Vector2 waypoint = waypoints.get(waypointIndex);
    scratch.set(target.x(), target.y());
    boolean reached = Motion.moveTowards(scratch, waypoint, speed * delta);
    target.moveTo(scratch.x, scratch.y);
    if (!reached) return;

    if (waypointIndex < waypoints.size() - 1) {
      waypointIndex++;
    } else {
      completed = true;
      onCompleted.emit();
    }
  }

  /** {@code true} once the final waypoint has been reached. */
  public boolean isCompleted() {
    return completed;
  }

  /** Index into the constructor's waypoint list currently being approached. */
  public int waypointIndex() {
    return waypointIndex;
  }

  /** Fires exactly once, when the final waypoint is reached. */
  public Signal0 onCompleted() {
    return onCompleted;
  }
}
