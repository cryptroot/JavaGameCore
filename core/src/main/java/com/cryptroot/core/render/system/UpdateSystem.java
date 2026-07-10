package com.cryptroot.core.render.system;

import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;

/** Calls {@link UpdateComponent#update(float)} on every entity that has one. */
public final class UpdateSystem {

  public void update(World world, float delta) {
    for (WorldEntity e : world.entities()) {
      e.get(UpdateComponent.class).ifPresent(c -> c.update(delta));
    }
  }
}
