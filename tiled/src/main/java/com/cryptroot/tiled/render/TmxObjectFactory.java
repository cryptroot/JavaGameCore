package com.cryptroot.tiled.render;

import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.tiled.model.ObjectGroup;
import com.cryptroot.tiled.model.TmxObject;
import java.util.Optional;

/**
 * Turns a Tiled object into a world entity — the seam for spawning gameplay from a map's object
 * layers (spawn points, triggers, collision regions, markers).
 *
 * <p>Object groups are parsed but never rendered; a game supplies one of these to {@link
 * TiledMap#spawnObjects} to interpret them. Typically the factory switches on the object's {@code
 * type()}/name or custom {@code properties()} and builds an appropriate {@link WorldEntity} (or
 * returns {@link Optional#empty()} to skip it).
 */
@FunctionalInterface
public interface TmxObjectFactory {

  /**
   * @return the entity to spawn for {@code object}, or empty to spawn nothing
   */
  Optional<WorldEntity> create(TmxObject object, ObjectGroup group);
}
