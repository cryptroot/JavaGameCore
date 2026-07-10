package com.cryptroot.core.world;

import com.badlogic.gdx.math.Vector3;

/**
 * Component that marks an entity as a point light source consumed by {@link
 * com.cryptroot.core.render.NormalMappedRenderer}.
 *
 * <p>{@link WorldEntityLayer#collectLights()} gathers all light positions from entities with this
 * component and passes them to the normal-mapped draw pass.
 */
public interface LightSourceComponent extends EntityComponent {
  /**
   * World-space light position: {@code x}, {@code y} are the ground-plane coordinates; {@code z} is
   * the height above the plane (e.g. 260f for a typical standing-height point light).
   */
  Vector3 lightPosition();

  /** Tinted colour of this light (RGB; values >1 are valid for HDR-style brightness). */
  Vector3 lightColor();

  /** Attenuation radius in world units — light reaches zero at this distance. */
  float radius();
}
