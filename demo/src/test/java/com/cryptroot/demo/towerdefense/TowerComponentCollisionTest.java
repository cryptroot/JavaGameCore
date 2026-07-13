package com.cryptroot.demo.towerdefense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.core.physics.BoxCollider;
import com.cryptroot.core.physics.Collider;
import com.cryptroot.core.physics.CollisionSystem;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.component.DefaultPositionComponent;
import com.cryptroot.core.world.component.HealthComponent;
import org.junit.jupiter.api.Test;

/**
 * GL-free test proving {@link TowerComponent}'s homing bullets deal damage <em>exactly once</em>
 * via {@link CollisionSystem} shape overlap, even in the coincidental-timing case where {@link
 * com.cryptroot.core.world.component.HomingProjectileComponent}'s own point-arrival also fires in
 * the same frame the collision is detected. {@code HomingProjectileComponent}'s {@code onImpact} is
 * wired as a no-op in {@link TowerComponent#fireAt} specifically to prevent that coincidence from
 * double-applying damage — see the note there.
 *
 * <p>Simulates the same per-frame order {@code core.screen.BaseGameScreen} uses: flush queued world
 * mutations, tick {@link UpdateComponent}s, then run {@link CollisionSystem#update}.
 */
class TowerComponentCollisionTest {

  private static TextureRegion fakeRegion() {
    return new TextureRegion() {
      @Override
      public int getRegionWidth() {
        return 16;
      }

      @Override
      public int getRegionHeight() {
        return 16;
      }
    };
  }

  /** Spawns an enemy-shaped entity (health + collider) whose collider spans exactly {@code box}. */
  private static WorldEntity spawnEnemy(World world, float x, float y, float size, int hp) {
    PositionComponent position = new DefaultPositionComponent(x, y);
    HealthComponent health = new HealthComponent(hp);
    BoxCollider collider = new BoxCollider(position, 0f, 0f, size, size);
    return world.add(
        new WorldEntity()
            .with(PositionComponent.class, position)
            .with(HealthComponent.class, health)
            .with(Collider.class, collider));
  }

  /** One frame: flush queued adds/removes, tick every {@link UpdateComponent}, then collide. */
  private static void runFrame(World world, CollisionSystem collisions, float delta) {
    world.flushAdditions();
    world.flushRemovals();
    for (WorldEntity e : world.entities()) {
      e.get(UpdateComponent.class).ifPresent(c -> c.update(delta));
    }
    collisions.update(world);
  }

  @Test
  void bulletDamagesTargetExactlyOnceDespiteSameFrameArrivalAndCollision() {
    World world = new World();
    float bulletSize = 1f;
    // Enemy collider spans the exact box the bullet spawns into (tower at the origin), so the
    // bullet's own arrival check ("reached" the target's position) and the collision overlap both
    // become true on the same tick — the coincidence this test targets.
    WorldEntity enemy = spawnEnemy(world, -bulletSize / 2f, -bulletSize / 2f, bulletSize, 100);
    HealthComponent enemyHealth = enemy.get(HealthComponent.class).orElseThrow();

    TowerComponent tower =
        new TowerComponent(
            world,
            0f,
            0f,
            /* range= */ 10f,
            /* shotsPerSecond= */ 1f,
            fakeRegion(),
            bulletSize,
            /* bulletSpeed= */ 1000f,
            /* damage= */ 34);
    // Fire exactly one bullet directly (the tower is deliberately never registered as a world
    // entity, so nothing ticks it again) — isolates "does a single bullet ever double-apply its
    // own damage", as opposed to a fast-firing tower legitimately landing several separate hits.
    tower.update(0.1f);

    CollisionSystem collisions = new CollisionSystem();
    for (int frame = 0; frame < 4; frame++) {
      runFrame(world, collisions, 0.1f);
    }

    assertEquals(66, enemyHealth.hp(), "damage must be applied exactly once, not doubled");
    assertTrue(enemyHealth.isAlive());
  }

  @Test
  void bulletDespawnsAfterCollisionImpact() {
    World world = new World();
    float bulletSize = 1f;
    spawnEnemy(world, -bulletSize / 2f, -bulletSize / 2f, bulletSize, 100);

    TowerComponent tower =
        new TowerComponent(world, 0f, 0f, 10f, 1f, fakeRegion(), bulletSize, 1000f, 34);
    tower.update(0.1f); // fires exactly one bullet — see the test above for why

    CollisionSystem collisions = new CollisionSystem();
    for (int frame = 0; frame < 4; frame++) {
      runFrame(world, collisions, 0.1f);
    }

    boolean anyBulletRemains =
        world.entities().stream()
            .anyMatch(e -> e.has(Collider.class) && !e.has(HealthComponent.class));
    assertFalse(anyBulletRemains, "the bullet entity must be removed after impact");
  }
}
