package com.cryptroot.core.world;

/** Component that contributes per-frame logic to its entity. */
public interface UpdateComponent extends EntityComponent {
  void update(float delta);
}
