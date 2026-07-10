package com.cryptroot.core.story;

import com.cryptroot.core.dialogue.DialogueBlackboard;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Save-backed {@link DialogueBlackboard} that holds the persistent story state for a game: boolean
 * flags (e.g. {@code "intro_done"}) and integer variables.
 *
 * <p>Requirement satisfaction is delegated to a pluggable resolver — typically wired by a {@link
 * StoryDirector} to a {@link RequirementTracker} so that {@link
 * com.cryptroot.core.dialogue.DialogueNode.Action} nodes and the director's polling share one
 * source of truth.
 *
 * <p>Flags and vars are exposed as live maps for save/restore round-tripping.
 */
public final class StoryState implements DialogueBlackboard {

  private final Map<String, Boolean> flags = new HashMap<>();
  private final Map<String, Integer> vars = new HashMap<>();

  /** Resolves gameplay requirement keys; defaults to "never satisfied". */
  private Predicate<String> requirementResolver = key -> false;

  @Override
  public boolean flag(String key) {
    return flags.getOrDefault(key, false);
  }

  @Override
  public void setFlag(String key, boolean v) {
    flags.put(key, v);
  }

  @Override
  public int var(String key) {
    return vars.getOrDefault(key, 0);
  }

  @Override
  public void setVar(String key, int v) {
    vars.put(key, v);
  }

  @Override
  public boolean requirementSatisfied(String key) {
    return requirementResolver.test(key);
  }

  /** Installs the resolver used by {@link #requirementSatisfied(String)}. */
  public void setRequirementResolver(Predicate<String> resolver) {
    this.requirementResolver = resolver != null ? resolver : (key -> false);
  }

  /** Live flag map — for save/restore. */
  public Map<String, Boolean> flags() {
    return flags;
  }

  /** Live var map — for save/restore. */
  public Map<String, Integer> vars() {
    return vars;
  }

  /** Replaces all flags/vars from a loaded save. Null arguments are ignored. */
  public void restore(Map<String, Boolean> savedFlags, Map<String, Integer> savedVars) {
    flags.clear();
    vars.clear();
    if (savedFlags != null) flags.putAll(savedFlags);
    if (savedVars != null) vars.putAll(savedVars);
  }
}
