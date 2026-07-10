package com.cryptroot.core.world.component;

import com.cryptroot.core.world.PositionComponent;

/**
 * Simple mutable world-space position for entities that have no render or bounds component.
 *
 * <p>Use this for purely logical entities (waypoints, script anchors, invisible triggers) that need
 * a world position but do not render anything. Spine-backed entities should use the {@link
 * PositionComponent} implementation built into {@link SpineRenderComponent} or {@link
 * NormalMappedRenderComponent} instead, to avoid two sources of truth.
 */
public final class DefaultPositionComponent implements PositionComponent {

  private float x;
  private float y;

  public DefaultPositionComponent(float x, float y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public float x() {
    return x;
  }

  @Override
  public float y() {
    return y;
  }

  @Override
  public void moveTo(float x, float y) {
    this.x = x;
    this.y = y;
  }
}
