package com.cryptroot.core.world;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A game-world object defined entirely by its attached components.
 *
 * <p>Components are stored in a {@code Class → EntityComponent} map. Calling {@link #with}
 * registers the component under the supplied key <em>and</em> automatically registers it under
 * every {@link EntityComponent} sub-interface it implements, so a single {@link
 * com.cryptroot.core.world.component.SpineRenderComponent} can be retrieved via {@code
 * entity.get(RenderComponent.class)}, {@code entity.get(OutlineableComponent.class)}, {@code
 * entity.get(PositionComponent.class)}, etc.
 *
 * <p>Auto-registration uses {@code putIfAbsent}: explicit registrations always win over
 * automatically discovered ones, and the first call for a given interface key is preserved if
 * called multiple times.
 *
 * <h3>Fluent construction example</h3>
 *
 * <pre>{@code
 * WorldEntity floor = new WorldEntity()
 *     .with(RenderComponent.class, new PolygonTileRenderComponent(...));
 *
 * WorldEntity florence = new WorldEntity()
 *     .with(SpineRenderComponent.class, spineComp)
 *     .with(BoundsComponent.class,      new SpineBoundsComponent(instance))
 *     .with(ClickableComponent.class,   new DefaultClickableComponent());
 * }</pre>
 */
public final class WorldEntity {

  private final Map<Class<? extends EntityComponent>, EntityComponent> components =
      new LinkedHashMap<>();

  /**
   * Adds {@code component} to this entity under {@code type} and auto-registers it under every
   * {@link EntityComponent} sub-interface it implements.
   *
   * @return {@code this} for fluent chaining
   */
  public <T extends EntityComponent> WorldEntity with(Class<T> type, T component) {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(component, "component must not be null");
    components.put(type, component);
    autoRegister(component);
    return this;
  }

  /**
   * Returns the component registered under {@code type}, or empty if none. Works for both concrete
   * types and interface types (thanks to auto-registration).
   */
  @SuppressWarnings("unchecked")
  public <T extends EntityComponent> Optional<T> get(Class<T> type) {
    Objects.requireNonNull(type, "type must not be null");
    return Optional.ofNullable((T) components.get(type));
  }

  /** Returns {@code true} if a component is registered under {@code type}. */
  public <T extends EntityComponent> boolean has(Class<T> type) {
    Objects.requireNonNull(type, "type must not be null");
    return components.containsKey(type);
  }

  /** All registered components in insertion order (without duplicates). */
  public Collection<EntityComponent> all() {
    return components.values();
  }

  // -------------------------------------------------------------------------
  // Reflection-based auto-registration
  // -------------------------------------------------------------------------

  private void autoRegister(EntityComponent component) {
    walkInterfaces(component.getClass(), component);
  }

  @SuppressWarnings("unchecked")
  private void walkInterfaces(Class<?> cls, EntityComponent component) {
    if (cls == null || cls == Object.class) return;
    for (Class<?> iface : cls.getInterfaces()) {
      if (EntityComponent.class.isAssignableFrom(iface) && iface != EntityComponent.class) {
        // putIfAbsent: explicit with() calls take priority
        components.putIfAbsent((Class<? extends EntityComponent>) iface, component);
      }
      walkInterfaces(iface, component);
    }
    walkInterfaces(cls.getSuperclass(), component);
  }
}
