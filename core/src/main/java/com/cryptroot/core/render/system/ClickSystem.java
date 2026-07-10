package com.cryptroot.core.render.system;

import com.cryptroot.core.world.BoundsComponent;
import com.cryptroot.core.world.ClickableComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import java.util.Optional;

/**
 * Finds the topmost entity under a world-space click, fires {@link ClickableComponent#onClicked()},
 * and returns the hit entity.
 */
public final class ClickSystem {

  /**
   * Performs a hit-test against entities with {@link BoundsComponent} in reverse-insertion order
   * (topmost first). Fires {@link ClickableComponent#onClicked()} if the entity also has one.
   *
   * @return the hit entity, or {@link Optional#empty()} if nothing was hit
   */
  public Optional<WorldEntity> handleClick(World world, float worldX, float worldY) {
    var entities = world.entities();
    for (int i = entities.size() - 1; i >= 0; i--) {
      WorldEntity e = entities.get(i);
      if (!e.has(BoundsComponent.class)) continue;
      if (!e.get(BoundsComponent.class).get().containsPoint(worldX, worldY)) continue;
      e.get(ClickableComponent.class).ifPresent(c -> c.onClicked().emit());
      return Optional.of(e);
    }
    return Optional.empty();
  }
}
