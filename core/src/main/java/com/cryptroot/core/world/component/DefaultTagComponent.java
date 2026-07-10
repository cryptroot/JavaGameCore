package com.cryptroot.core.world.component;

import com.cryptroot.core.world.TagComponent;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Immutable tag set for an entity.
 *
 * <pre>{@code
 * entity.with(TagComponent.class, new DefaultTagComponent("unit", "player"));
 * }</pre>
 */
public final class DefaultTagComponent implements TagComponent {

  private final Set<String> tags;

  public DefaultTagComponent(String... tags) {
    this.tags = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(tags)));
  }

  @Override
  public boolean hasTag(String tag) {
    return tags.contains(tag);
  }

  @Override
  public Set<String> tags() {
    return tags;
  }
}
