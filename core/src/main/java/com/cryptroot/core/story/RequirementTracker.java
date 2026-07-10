package com.cryptroot.core.story;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Registry mapping abstract story requirement keys (e.g. {@code "reach_day_2"}) to predicates over
 * a game-specific context type {@code C}.
 *
 * <p>A {@link StoryDirector} consults this tracker both when a {@link
 * com.cryptroot.core.dialogue.DialogueNode.Action} is reached and when {@link StoryDirector#poll()}
 * is called, so paused conversations resume automatically the moment their requirement becomes
 * true.
 *
 * <p>The context type {@code C} is intentionally unbound — it may be a game state object, an
 * interface, or any other application-specific type that the predicates need to inspect.
 *
 * @param <C> the game-context type whose state predicates interrogate
 */
public final class RequirementTracker<C> {

  private final Map<String, Predicate<C>> requirements = new HashMap<>();

  /** Registers (or replaces) the predicate for {@code key}. */
  public RequirementTracker<C> register(String key, Predicate<C> predicate) {
    requirements.put(key, predicate);
    return this;
  }

  /** Returns {@code true} if a predicate is registered for {@code key}. */
  public boolean has(String key) {
    return requirements.containsKey(key);
  }

  /**
   * Evaluates the predicate for {@code key} against {@code ctx}. Unknown keys are treated as
   * <em>not</em> satisfied.
   */
  public boolean isSatisfied(String key, C ctx) {
    Predicate<C> p = requirements.get(key);
    return p != null && p.test(ctx);
  }
}
