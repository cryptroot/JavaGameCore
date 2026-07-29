package com.cryptroot.core.records;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The identity of one record: its {@link RecordComponent}s rendered as a canonical, parseable
 * string.
 *
 * <p>The form is {@code dimension=value} pairs joined by {@code '|'} and ordered by dimension id:
 *
 * <pre>{@code
 * content=blood|context=bank|measure=times|origin=player
 * }</pre>
 *
 * <p>Three properties fall out of that design, which are important to the record system's
 * correctness and performance:
 *
 * <ul>
 *   <li><b>Round-trips.</b> {@link #parse} recovers the exact components, so a key read back from a
 *       save is still self-describing even when no definition for it survives.
 *   <li><b>Cannot collide.</b> The dimension is in the key, so the same value on two different axes
 *       ({@code content=gold} and {@code source=gold}) can never produce the same key.
 *   <li><b>Stable prefix.</b> Ordering by dimension rather than by value means changing one
 *       component does not reshuffle the whole key, so two related records still share a readable
 *       prefix.
 * </ul>
 *
 * <p>Keys are order-independent by construction: components sort by dimension, so supply order
 * never matters. Deliberately a typed value rather than a bare {@code String} — a typo can no
 * longer create a counter that every query silently ignores.
 *
 * <p>This deliberately does not cache its parsed components. A key is a hot map key, so {@code
 * equals}/{@code hashCode} stay a single string comparison; component questions belong to the
 * {@link RecordDefinition} the {@link RecordKeeper} holds, not to the key.
 *
 * @param id the canonical rendering, never {@code null}
 */
public record RecordKey(String id) implements Comparable<RecordKey> {

  /** Separates one {@code dimension=value} pair from the next. */
  public static final String PAIR_SEPARATOR = "|";

  /** Separates a dimension from its value inside one pair. */
  public static final String ASSIGNMENT = "=";

  /**
   * Validates that {@code id} is exactly the canonical rendering of the components it describes.
   *
   * <p>The check is a round-trip: {@code id} is parsed and re-rendered, and anything but an exact
   * match is rejected. So {@link #components()} can never disagree with {@link #id()}, and a
   * mis-ordered or malformed key cannot exist.
   *
   * @throws NullPointerException if {@code id} is {@code null}
   * @throws IllegalArgumentException if {@code id} is not canonical — malformed, empty, carrying
   *     two values on one dimension, or ordered by anything other than dimension id
   */
  public RecordKey {
    Objects.requireNonNull(id, "id must not be null");
    String canonical = render(parseComponents(id));
    if (!id.equals(canonical)) {
      throw new IllegalArgumentException(
          "key must be canonical: expected " + canonical + " but was " + id);
    }
  }

  /**
   * The key identifying the record classified by exactly {@code components}.
   *
   * @throws NullPointerException if {@code components} or any element is {@code null}
   * @throws IllegalArgumentException if {@code components} is empty or carries two components on
   *     the same {@link RecordDimension}
   */
  public static RecordKey of(Collection<? extends RecordComponent> components) {
    Objects.requireNonNull(components, "components must not be null");
    if (components.isEmpty()) {
      throw new IllegalArgumentException("components must not be empty");
    }
    SortedMap<RecordDimension, RecordComponent> byDimension = new TreeMap<>();
    for (RecordComponent component : components) {
      Objects.requireNonNull(component, "components must not contain null");
      RecordComponent canonical = RecordComponent.canonical(component);
      RecordComponent existing = byDimension.putIfAbsent(canonical.dimension(), canonical);
      if (existing != null) {
        throw new IllegalArgumentException(
            "two components on dimension "
                + canonical.dimension()
                + ": "
                + existing.value()
                + " and "
                + canonical.value());
      }
    }
    return new RecordKey(render(byDimension));
  }

  /** The key identifying the record classified by exactly {@code components}. */
  public static RecordKey of(RecordComponent... components) {
    Objects.requireNonNull(components, "components must not be null");
    return of(List.of(components));
  }

  /**
   * The key {@code id} denotes — the entry point for keys arriving from outside the program,
   * chiefly save data.
   *
   * <p>Strict rather than fail-soft: a key that cannot be parsed back into components is not a
   * record key, so it is rejected here instead of becoming a counter nothing can ever explain.
   *
   * @throws NullPointerException if {@code id} is {@code null}
   * @throws IllegalArgumentException if {@code id} is not canonical
   */
  public static RecordKey parse(String id) {
    return new RecordKey(id);
  }

  /**
   * The components this key describes, in dimension order.
   *
   * <p>Parsed on demand, for diagnostics and for keys restored from a save whose definition no
   * longer exists. When a definition <em>is</em> available, prefer asking it — {@link
   * RecordDefinition#has} and {@link RecordKeeper#matches} are the query path.
   */
  public SortedMap<RecordDimension, RecordComponent> components() {
    return Collections.unmodifiableSortedMap(parseComponents(id));
  }

  /** Orders by {@link #id()}, which orders by dimension: related records sort together. */
  @Override
  public int compareTo(RecordKey other) {
    Objects.requireNonNull(other, "other must not be null");
    return id.compareTo(other.id);
  }

  /** The {@link #id()} — a key reads as its canonical form everywhere. */
  @Override
  public String toString() {
    return id;
  }

  private static String render(SortedMap<RecordDimension, RecordComponent> components) {
    StringBuilder rendered = new StringBuilder();
    for (RecordComponent component : components.values()) {
      if (rendered.length() > 0) {
        rendered.append(PAIR_SEPARATOR);
      }
      rendered.append(component.dimension().id()).append(ASSIGNMENT).append(component.value());
    }
    return rendered.toString();
  }

  /**
   * Splits {@code id} into components. No escaping is needed: {@link RecordDimension#CANONICAL}
   * excludes both separators, so every {@code =} and {@code |} in a well-formed key is structural.
   */
  private static SortedMap<RecordDimension, RecordComponent> parseComponents(String id) {
    if (id.isEmpty()) {
      throw new IllegalArgumentException("key must not be empty");
    }
    SortedMap<RecordDimension, RecordComponent> components = new TreeMap<>();
    for (String pair : id.split("\\" + PAIR_SEPARATOR, -1)) {
      int assignment = pair.indexOf(ASSIGNMENT);
      if (assignment < 0) {
        throw new IllegalArgumentException(
            "key part \"" + pair + "\" must be dimension" + ASSIGNMENT + "value, in key " + id);
      }
      RecordComponent component =
          RecordComponent.of(
              RecordDimension.of(pair.substring(0, assignment)), pair.substring(assignment + 1));
      RecordComponent existing = components.putIfAbsent(component.dimension(), component);
      if (existing != null) {
        throw new IllegalArgumentException(
            "key "
                + id
                + " has two values on dimension "
                + component.dimension()
                + ": "
                + existing.value()
                + " and "
                + component.value());
      }
    }
    return components;
  }
}
