package com.cryptroot.core.world;

/**
 * Marker component declaring that an entity should <em>always</em> show its selection outline,
 * independent of pointer hover.
 *
 * <p>The {@link com.cryptroot.core.render.system.OutlineRenderSystem} captures every entity
 * carrying this marker (alongside the current hover target) into the {@link
 * com.cryptroot.core.render.SelectionOutlineRenderer} FBO so a persistent outline can be drawn
 * around it. The entity must also expose a {@link RenderComponent} (or a texture/Spine outlineable
 * component) so there is something to capture.
 *
 * <p>Being a pure marker, a single shared {@link #INSTANCE} can be registered on any number of
 * entities:
 *
 * <pre>{@code
 * world.add(new WorldEntity()
 *     .with(RenderComponent.class,        sprite)
 *     .with(AlwaysOutlinedComponent.class, AlwaysOutlinedComponent.INSTANCE));
 * }</pre>
 */
public interface AlwaysOutlinedComponent extends EntityComponent {

  /**
   * Shared stateless marker instance — register this on any entity that needs an always-on outline.
   */
  AlwaysOutlinedComponent INSTANCE = new AlwaysOutlinedComponent() {};
}
