package com.cryptroot.tiled.render;

import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.tiled.model.ObjectGroup;
import com.cryptroot.tiled.model.TmxMap;
import com.cryptroot.tiled.model.TmxObject;
import java.util.ArrayList;
import java.util.List;

/**
 * A loaded, render-ready Tiled map: the parsed {@link TmxMap model}, the {@link TileAtlas} of its
 * tiles, and one {@link TileLayerRenderComponent} per tile layer in document (draw) order.
 *
 * <p>Obtain an instance from {@link TiledMapLoader}. Add its layers to a scene with {@link
 * #addTo(World)}; object groups are exposed through {@link #model()} for consumers to interpret
 * (spawn points, triggers, collision regions, …) and are not rendered automatically.
 *
 * <p>Textures are owned by the {@code ResourceManager} that loaded the map, so there is nothing to
 * dispose here.
 */
public final class TiledMap {

  private final TmxMap model;
  private final TileAtlas atlas;
  private final List<TileLayerRenderComponent> layerComponents;

  /**
   * @param model the parsed map
   * @param atlas the gid-to-region atlas
   * @param layerComponents render components for the tile layers, in document order
   */
  public TiledMap(TmxMap model, TileAtlas atlas, List<TileLayerRenderComponent> layerComponents) {
    this.model = model;
    this.atlas = atlas;
    this.layerComponents = List.copyOf(layerComponents);
  }

  /**
   * @return the parsed map model (including object groups and properties).
   */
  public TmxMap model() {
    return model;
  }

  /**
   * @return the tile atlas backing this map's render components.
   */
  public TileAtlas atlas() {
    return atlas;
  }

  /**
   * @return the tile-layer render components, in document (draw) order.
   */
  public List<TileLayerRenderComponent> layerComponents() {
    return layerComponents;
  }

  /**
   * Adds one entity per tile layer to {@code world}, in document order, so that the layers draw
   * back-to-front through the core render pipeline.
   *
   * @param world the world to populate
   * @return the entities created, one per tile layer, in document order
   */
  public List<WorldEntity> addTo(World world) {
    List<WorldEntity> added = new ArrayList<>(layerComponents.size());
    for (TileLayerRenderComponent layer : layerComponents) {
      added.add(world.add(new WorldEntity().with(RenderComponent.class, layer)));
    }
    return added;
  }

  /**
   * Spawns world entities from this map's object layers by handing each {@link TmxObject} (with its
   * owning {@link ObjectGroup}) to {@code factory}. Objects in document order across all object
   * groups; entities the factory returns are {@link World#add added} to {@code world}. Objects the
   * factory declines (empty result) are skipped.
   *
   * @return the entities spawned, in the order they were created
   */
  public List<WorldEntity> spawnObjects(World world, TmxObjectFactory factory) {
    List<WorldEntity> spawned = new ArrayList<>();
    for (ObjectGroup group : model.objectGroups()) {
      for (TmxObject object : group.objects()) {
        factory.create(object, group).ifPresent(e -> spawned.add(world.add(e)));
      }
    }
    return spawned;
  }
}
