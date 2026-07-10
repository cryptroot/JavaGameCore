package com.cryptroot.core.world;

import java.util.Set;

/**
 * Component that attaches arbitrary string tags to an entity.
 *
 * <p>Tags enable semantic lookup via {@link WorldEntityLayer#findByTag(String)}, allowing scripts
 * and game systems to reference entities by name rather than by Java reference (e.g. {@code
 * "player"}, {@code "enemy"}, {@code "interactable"}).
 */
public interface TagComponent extends EntityComponent {
  boolean hasTag(String tag);

  Set<String> tags();
}
