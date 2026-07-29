package com.cryptroot.core.records;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The registry of every defined record: it mints a record's key from its components and is then the
 * single place that can answer what a key means.
 *
 * <p>This is deliberately <em>not</em> a singleton. Construct one per game session and hand it to
 * whoever needs it (a field on your {@code GameContext} subclass, injected into screens and into
 * every {@link RecordBookComponent}) — the same rule the engine applies to all shared services.
 *
 * <p>Every record a game counts should be defined here, including the members of a {@link
 * RecordTemplate} family, which register themselves on first use. A counter under an undefined key
 * still works, but nothing can name it or find it by component, so the soft lookups below exist
 * mainly for keys restored from a save whose definition has since been removed.
 *
 * <p>Not thread-safe: definitions are expected to be registered during start-up from one thread,
 * and read afterwards. Iteration order of {@link #definitions()} and {@link #keysMatching} is
 * definition order.
 */
public final class RecordKeeper {

  private final Map<RecordKey, RecordDefinition> registry = new LinkedHashMap<>();

  /**
   * Registers {@code definition} and returns its key.
   *
   * @throws NullPointerException if {@code definition} is {@code null}
   * @throws IllegalStateException if a record with the same key is already defined — two records
   *     that share the same set of components are the same record, so this is a content bug rather
   *     than a recoverable condition
   */
  public RecordKey define(RecordDefinition definition) {
    Objects.requireNonNull(definition, "definition must not be null");
    RecordKey key = definition.key();
    RecordDefinition existing = registry.putIfAbsent(key, definition);
    if (existing != null) {
      throw new IllegalStateException("record with key " + key + " is already defined");
    }
    return key;
  }

  /**
   * Registers {@code definition} if its key is not already taken, and returns that key either way.
   *
   * <p>The idempotent counterpart to {@link #define}, for records discovered as play happens rather
   * than listed in a catalogue — see {@link RecordTemplate#define}. An already-registered
   * definition is left in place; this never replaces one.
   *
   * @throws NullPointerException if {@code definition} is {@code null}
   */
  public RecordKey defineIfAbsent(RecordDefinition definition) {
    Objects.requireNonNull(definition, "definition must not be null");
    RecordKey key = definition.key();
    registry.putIfAbsent(key, definition);
    return key;
  }

  /** {@code true} if a record is defined under {@code key}. */
  public boolean isDefined(RecordKey key) {
    Objects.requireNonNull(key, "key must not be null");
    return registry.containsKey(key);
  }

  /**
   * The definition registered under {@code key}, or empty if none — the soft lookup, for callers
   * that legitimately hold keys with no definition, chiefly keys read back from an older save.
   */
  public Optional<RecordDefinition> find(RecordKey key) {
    Objects.requireNonNull(key, "key must not be null");
    return Optional.ofNullable(registry.get(key));
  }

  /**
   * The definition registered under {@code key}.
   *
   * @throws IllegalArgumentException if no record is defined under {@code key}
   */
  public RecordDefinition get(RecordKey key) {
    Objects.requireNonNull(key, "key must not be null");
    RecordDefinition definition = registry.get(key);
    if (definition == null) {
      throw new IllegalArgumentException("no record found with key " + key);
    }
    return definition;
  }

  /**
   * The display name of the record under {@code key}.
   *
   * @throws IllegalArgumentException if no record is defined under {@code key}
   * @throws IllegalStateException if the record is defined but has no name
   */
  public String getName(RecordKey key) {
    RecordDefinition definition = get(key);
    if (definition.name() == null) {
      throw new IllegalStateException("no name found for record with key " + key);
    }
    return definition.name();
  }

  /**
   * The description of the record under {@code key}.
   *
   * @throws IllegalArgumentException if no record is defined under {@code key}
   * @throws IllegalStateException if the record is defined but has no description
   */
  public String getDescription(RecordKey key) {
    RecordDefinition definition = get(key);
    if (definition.description() == null) {
      throw new IllegalStateException("no description found for record with key " + key);
    }
    return definition.description();
  }

  /**
   * {@code true} if the record under {@code key} satisfies {@code query}.
   *
   * <p>Fail-soft, documented: an undefined {@code key} matches nothing and returns {@code false}
   * rather than throwing, so a caller can query straight across a counter bag restored from a save
   * without first proving every key still has a definition.
   */
  public boolean matches(RecordKey key, RecordQuery query) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(query, "query must not be null");
    RecordDefinition definition = registry.get(key);
    return definition != null && definition.matches(query);
  }

  /** The keys of every defined record satisfying {@code query}, in definition order. */
  public List<RecordKey> keysMatching(RecordQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return keysMatching(query, registry.keySet());
  }

  /** The keys of every defined record carrying {@code component}, in definition order. */
  public List<RecordKey> keysMatching(RecordComponent component) {
    return keysMatching(RecordQuery.has(component));
  }

  /**
   * The subset of {@code keys} whose records satisfy {@code query}, in the iteration order of
   * {@code keys}.
   *
   * <p>Fail-soft, documented: keys with no definition are skipped rather than rejected, for the
   * same reason {@link #matches} returns {@code false} for them.
   *
   * @throws NullPointerException if {@code query}, {@code keys}, or any key is {@code null}
   */
  public List<RecordKey> keysMatching(RecordQuery query, Collection<RecordKey> keys) {
    Objects.requireNonNull(query, "query must not be null");
    Objects.requireNonNull(keys, "keys must not be null");
    List<RecordKey> matching = new ArrayList<>();
    for (RecordKey key : keys) {
      Objects.requireNonNull(key, "keys must not contain null");
      RecordDefinition definition = registry.get(key);
      if (definition != null && definition.matches(query)) {
        matching.add(key);
      }
    }
    return List.copyOf(matching);
  }

  /** The subset of {@code keys} whose records carry {@code component}. */
  public List<RecordKey> keysMatching(RecordComponent component, Collection<RecordKey> keys) {
    return keysMatching(RecordQuery.has(component), keys);
  }

  /** An unmodifiable live view of every definition, keyed by record key, in definition order. */
  public Map<RecordKey, RecordDefinition> definitions() {
    return Collections.unmodifiableMap(registry);
  }
}
