package com.cryptroot.demo.towerdefense;

import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.time.Motion;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import java.util.List;
import java.util.Objects;

/**
 * Game-specific enemy behaviour: follows a precomputed path of world-space waypoints from the
 * bottom of the arena toward the top, and tracks hit points applied by {@link BulletComponent}.
 *
 * <p>The path is computed once at spawn time (see {@code EnemySpawnerComponent}) and is not
 * replanned if towers are placed afterwards — a deliberate simplification for this demo.
 */
final class EnemyComponent implements UpdateComponent {

  private final World world;
  private final PositionComponent position;
  private final List<Vector2> waypoints;
  private final float speed;
  private final Vector2 scratch = new Vector2();

  private WorldEntity self;
  private int waypointIndex;
  private int hp;
  private boolean alive = true;

  EnemyComponent(
      World world, PositionComponent position, List<Vector2> waypoints, float speed, int hp) {
    this.world = Objects.requireNonNull(world, "world must not be null");
    this.position = Objects.requireNonNull(position, "position must not be null");
    this.waypoints = Objects.requireNonNull(waypoints, "waypoints must not be null");
    if (waypoints.isEmpty()) {
      throw new IllegalArgumentException("waypoints must not be empty");
    }
    if (speed <= 0f) {
      throw new IllegalArgumentException("speed must be positive: " + speed);
    }
    if (hp <= 0) {
      throw new IllegalArgumentException("hp must be positive: " + hp);
    }
    this.speed = speed;
    this.hp = hp;
  }

  /** Binds the owning entity, required for self-removal on death or reaching the goal. */
  void bind(WorldEntity self) {
    this.self = Objects.requireNonNull(self, "self must not be null");
  }

  @Override
  public void update(float delta) {
    if (!alive) return;
    Vector2 target = waypoints.get(waypointIndex);
    scratch.set(position.x(), position.y());
    boolean reached = Motion.moveTowards(scratch, target, speed * delta);
    position.moveTo(scratch.x, scratch.y);
    if (!reached) return;

    if (waypointIndex < waypoints.size() - 1) {
      waypointIndex++;
    } else {
      // Reached the final waypoint at the top of the arena — leaked through.
      despawn();
    }
  }

  /** Applies {@code amount} damage; despawns the entity once hit points are exhausted. */
  void damage(int amount) {
    if (!alive) return;
    hp -= amount;
    if (hp <= 0) despawn();
  }

  boolean isAlive() {
    return alive;
  }

  private void despawn() {
    alive = false;
    world.queueRemove(self);
  }
}
