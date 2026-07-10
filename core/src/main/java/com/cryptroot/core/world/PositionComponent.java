package com.cryptroot.core.world;

/**
 * Component that tracks the canonical world-space position of an entity.
 *
 * <p>For Spine-backed entities, this is implemented directly by {@link
 * com.cryptroot.core.world.component.SpineRenderComponent} and {@link
 * com.cryptroot.core.world.component.NormalMappedRenderComponent} — their {@link #moveTo} delegates
 * to {@code SpineUnitInstance.setPosition()}, avoiding two sources of truth.
 *
 * <p>For entities that have no render component (trigger zones, waypoints), use {@link
 * com.cryptroot.core.world.component.DefaultPositionComponent}.
 */
public interface PositionComponent extends EntityComponent {
  float x();

  float y();

  void moveTo(float x, float y);
}
