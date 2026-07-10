package com.cryptroot.core.world;

import com.cryptroot.core.event.Signal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Thin entity container for a scene.
 *
 * <p>Replaces the old {@link WorldEntityLayer} as the canonical entity store. All per-frame logic
 * (updates, hover, rendering) has been extracted to dedicated systems in {@link
 * com.cryptroot.core.render.system}.
 *
 * <p>Use {@link com.cryptroot.core.render.RenderPipeline} to drive the full frame pipeline for a
 * world-camera scene, or invoke systems individually for simpler screens.
 */
public final class World {

  private final List<WorldEntity> entities = new ArrayList<>();
  private final List<WorldEntity> view = Collections.unmodifiableList(entities);

  /** Entities queued for deferred removal; applied by {@link #flushRemovals()}. */
  private final List<WorldEntity> pendingRemoval = new ArrayList<>();

  /** Fires once per entity actually removed (immediate or flushed). */
  private final Signal<WorldEntity> onRemoved = new Signal<>();

  /**
   * Adds {@code entity} to this world and returns it for fluent post-wiring:
   *
   * <pre>{@code
   * WorldEntity e = world.add(new WorldEntity()
   *     .with(SpineRenderComponent.class, spineComp));
   * }</pre>
   */
  public WorldEntity add(WorldEntity entity) {
    entities.add(entity);
    return entity;
  }

  /**
   * Returns an unmodifiable live view of all entities in insertion order. Systems iterate this list
   * directly — do not modify it during iteration.
   */
  public List<WorldEntity> entities() {
    return view;
  }

  /** Returns all entities that carry the given tag. */
  public List<WorldEntity> findByTag(String tag) {
    return entities.stream()
        .filter(e -> e.has(TagComponent.class) && e.get(TagComponent.class).get().hasTag(tag))
        .collect(Collectors.toList());
  }

  /** Returns {@code true} if at least one entity has a component of the given type. */
  public boolean hasEntitiesWith(Class<? extends EntityComponent> componentType) {
    for (WorldEntity e : entities) {
      if (e.has(componentType)) return true;
    }
    return false;
  }

  /**
   * Removes {@code entity} immediately and fires {@link #onRemoved()}.
   *
   * <p>Safe only <em>outside</em> the per-frame system loop (e.g. from input handlers or between
   * frames, such as toggling a prep-phase placement). To remove an entity from inside a system's
   * {@code update()} — while the live {@link #entities()} view is being iterated — use {@link
   * #queueRemove} instead.
   *
   * @return {@code true} if the entity was present and removed
   */
  public boolean remove(WorldEntity entity) {
    boolean removed = entities.remove(entity);
    if (removed) onRemoved.emit(entity);
    return removed;
  }

  /**
   * Queues {@code entity} for removal on the next {@link #flushRemovals()}. Safe to call while
   * systems iterate {@link #entities()} (e.g. an entity removing itself from its own {@code
   * update()}). Duplicate queue requests for the same entity collapse to a single removal.
   */
  public void queueRemove(WorldEntity entity) {
    pendingRemoval.add(entity);
  }

  /**
   * Applies all queued removals, firing {@link #onRemoved()} once per entity that was actually
   * present. Call at a point where no system is iterating the entity list — {@link
   * com.cryptroot.core.render.RenderPipeline#update} does this at the start of each frame.
   */
  public void flushRemovals() {
    if (pendingRemoval.isEmpty()) return;
    for (WorldEntity e : pendingRemoval) {
      if (entities.remove(e)) onRemoved.emit(e);
    }
    pendingRemoval.clear();
  }

  /** Despawn signal: fires with each entity as it is removed. */
  public Signal<WorldEntity> onRemoved() {
    return onRemoved;
  }

  /**
   * Removes all entities. Does <em>not</em> reset hover or render state — call {@link
   * com.cryptroot.core.render.RenderPipeline#reset()} or {@link
   * com.cryptroot.core.render.system.HoverSystem#reset()} separately.
   *
   * <p>Does not fire {@link #onRemoved()} (bulk teardown, not per-entity despawn) and discards any
   * pending queued removals.
   */
  public void clear() {
    entities.clear();
    pendingRemoval.clear();
  }
}
