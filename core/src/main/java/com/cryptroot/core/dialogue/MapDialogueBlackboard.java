package com.cryptroot.core.dialogue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A simple in-memory {@link DialogueBlackboard} backed by hash maps.
 *
 * <p>Used by the legacy linear-script path of {@link com.cryptroot.core.ui.ConversationWidget} and
 * by unit tests. Requirement satisfaction is driven manually via {@link #setRequirementSatisfied}.
 */
public final class MapDialogueBlackboard implements DialogueBlackboard {

  private final Map<String, Boolean> flags = new HashMap<>();
  private final Map<String, Integer> vars = new HashMap<>();
  private final Set<String> satisfied = new HashSet<>();

  @Override
  public boolean flag(String key) {
    Objects.requireNonNull(key, "key must not be null");
    return flags.getOrDefault(key, false);
  }

  @Override
  public void setFlag(String key, boolean v) {
    Objects.requireNonNull(key, "key must not be null");
    flags.put(key, v);
  }

  @Override
  public int var(String key) {
    Objects.requireNonNull(key, "key must not be null");
    return vars.getOrDefault(key, 0);
  }

  @Override
  public void setVar(String key, int v) {
    Objects.requireNonNull(key, "key must not be null");
    vars.put(key, v);
  }

  @Override
  public boolean requirementSatisfied(String key) {
    Objects.requireNonNull(key, "key must not be null");
    return satisfied.contains(key);
  }

  /** Marks {@code key} as satisfied or not — for tests / manual control. */
  public void setRequirementSatisfied(String key, boolean value) {
    Objects.requireNonNull(key, "key must not be null");
    if (value) satisfied.add(key);
    else satisfied.remove(key);
  }
}
