package com.cryptroot.tiled.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.tiled.io.TmxParser;
import com.cryptroot.tiled.model.TmxMap;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TiledMapSpawnObjectsTest {

  // The Embedded.tmx fixture has one object group with 3 objects; only one is
  // type "PlayerStart". No atlas/layers are needed to spawn from object groups.
  private TiledMap mapOnly() throws IOException {
    TmxMap model = new TmxParser().parse("assets/test/Embedded.tmx");
    return new TiledMap(model, null, List.of());
  }

  @Test
  void factorySelectivelySpawnsAndAddsToWorld() throws IOException {
    TiledMap map = mapOnly();
    World world = new World();

    List<WorldEntity> spawned =
        map.spawnObjects(
            world,
            (object, group) ->
                "PlayerStart".equals(object.type())
                    ? Optional.of(new WorldEntity())
                    : Optional.empty());

    assertEquals(1, spawned.size(), "only the PlayerStart object is accepted");
    assertEquals(1, world.entities().size(), "spawned entity added to the world");
  }

  @Test
  void emptyResultsSkipEverything() throws IOException {
    TiledMap map = mapOnly();
    World world = new World();
    List<WorldEntity> spawned = map.spawnObjects(world, (o, g) -> Optional.empty());
    assertEquals(0, spawned.size());
    assertEquals(0, world.entities().size());
  }

  @Test
  void acceptAllSpawnsEveryObject() throws IOException {
    TiledMap map = mapOnly();
    World world = new World();
    List<WorldEntity> spawned = map.spawnObjects(world, (o, g) -> Optional.of(new WorldEntity()));
    assertEquals(3, spawned.size(), "all three fixture objects spawned");
  }
}
