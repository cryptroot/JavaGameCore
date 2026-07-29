package com.cryptroot.core.records;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The immutable definition of one record: its generated {@link #key()}, the components that
 * classify it, and the optional display {@link #name()} / {@link #description()}.
 *
 * <p>{@code key} is not free-form — it must equal {@link RecordKey#of} applied to {@code
 * components}, and the constructor rejects any other value, so a definition can never disagree with
 * its own key. Build one through {@link #builder()}, which derives the key for you.
 *
 * <p>{@code name} and {@code description} are presentation-only, take no part in key generation,
 * and may be {@code null} when the record has none. Prefer the checked {@link RecordKeeper#getName}
 * / {@link RecordKeeper#getDescription} accessors when a caller requires them to be present.
 *
 * <p>Components are stored {@linkplain RecordComponent#canonical canonicalised}, so a definition
 * built from a game's enum and one built from {@link RecordComponent#of} naming the same axis and
 * value are equal and answer every query identically.
 *
 * <p>{@code aggregation} is how repeated observations combine — a plain counter by default, but
 * {@link RecordAggregation#MAX} or {@link RecordAggregation#MIN} for a high score or a best time.
 * It takes no part in the key either: a record is one statistic, so the same components cannot mean
 * both a sum and a maximum.
 *
 * @param key the generated identity of this record, never {@code null}
 * @param name human-readable title, or {@code null} if undefined
 * @param description longer explanation, or {@code null} if undefined
 * @param aggregation how observations of this record combine, never {@code null}
 * @param components one canonical component per {@link RecordDimension}, never empty, unmodifiable,
 *     iterating in dimension-id order
 */
public record RecordDefinition(
    RecordKey key,
    String name,
    String description,
    RecordAggregation aggregation,
    SortedMap<RecordDimension, RecordComponent> components) {

  /**
   * Validates the definition, canonicalises and defensively copies {@code components}.
   *
   * @throws NullPointerException if {@code key}, {@code aggregation} or {@code components} is
   *     {@code null}, or {@code components} contains a {@code null} key or value
   * @throws IllegalArgumentException if {@code components} is empty, a component is filed under a
   *     dimension other than its own, or {@code key} is not the key generated from {@code
   *     components}
   */
  public RecordDefinition {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(aggregation, "aggregation must not be null");
    Objects.requireNonNull(components, "components must not be null");
    if (components.isEmpty()) {
      throw new IllegalArgumentException("components must not be empty");
    }
    SortedMap<RecordDimension, RecordComponent> canonical = new TreeMap<>();
    components.forEach(
        (dimension, component) -> {
          Objects.requireNonNull(dimension, "components must not contain a null dimension");
          Objects.requireNonNull(component, "components must not contain a null component");
          if (!component.dimension().equals(dimension)) {
            throw new IllegalArgumentException(
                "component "
                    + component.value()
                    + " belongs to dimension "
                    + component.dimension()
                    + ", not "
                    + dimension);
          }
          canonical.put(dimension, RecordComponent.canonical(component));
        });
    // Map.copyOf would be unordered, which would make key derivation non-deterministic.
    components = Collections.unmodifiableSortedMap(canonical);
    RecordKey derived = RecordKey.of(components.values());
    if (!key.equals(derived)) {
      throw new IllegalArgumentException(
          "key must be derived from components: expected " + derived + " but was " + key);
    }
  }

  /** A fresh builder; the built definition's key is derived from the components you add. */
  public static Builder builder() {
    return new Builder();
  }

  /** The component classifying this record on {@code dimension}, or empty if it has none. */
  public Optional<RecordComponent> component(RecordDimension dimension) {
    Objects.requireNonNull(dimension, "dimension must not be null");
    return Optional.ofNullable(components.get(dimension));
  }

  /**
   * {@code true} if this record is classified by exactly {@code component}.
   *
   * <p>Compared by {@code (dimension, value)}, never by identity, so any implementation naming the
   * same axis and value matches — including one from a game this module has never heard of.
   */
  public boolean has(RecordComponent component) {
    Objects.requireNonNull(component, "component must not be null");
    RecordComponent present = components.get(component.dimension());
    return present != null && present.equals(RecordComponent.canonical(component));
  }

  /** {@code true} if this record satisfies {@code query}. */
  public boolean matches(RecordQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return query.test(this);
  }

  /** Fluent builder for a {@link RecordDefinition}. Not thread-safe; use one per definition. */
  public static final class Builder {

    private final SortedMap<RecordDimension, RecordComponent> components = new TreeMap<>();
    private String name;
    private String description;
    private RecordAggregation aggregation = RecordAggregation.SUM;

    private Builder() {}

    /**
     * Adds one classifying component.
     *
     * @throws NullPointerException if {@code component} is {@code null}
     * @throws IllegalArgumentException if a component was already added for the same {@link
     *     RecordDimension}
     */
    public Builder component(RecordComponent component) {
      Objects.requireNonNull(component, "component must not be null");
      RecordComponent canonical = RecordComponent.canonical(component);
      RecordComponent existing = components.putIfAbsent(canonical.dimension(), canonical);
      if (existing != null) {
        throw new IllegalArgumentException(
            "dimension "
                + component.dimension()
                + " already set to "
                + existing.value()
                + ", cannot also be "
                + component.value());
      }
      return this;
    }

    /** Adds several classifying components, in any order. */
    public Builder components(RecordComponent... toAdd) {
      Objects.requireNonNull(toAdd, "toAdd must not be null");
      for (RecordComponent component : toAdd) {
        component(component);
      }
      return this;
    }

    /**
     * Sets the human-readable title. Omit the call entirely to leave the record unnamed.
     *
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public Builder name(String name) {
      this.name = requireText(name, "name");
      return this;
    }

    /**
     * Sets the longer explanation. Omit the call entirely to leave the record undescribed.
     *
     * @throws IllegalArgumentException if {@code description} is blank
     */
    public Builder description(String description) {
      this.description = requireText(description, "description");
      return this;
    }

    /**
     * Builds the definition, deriving its key from the components added so far.
     *
     * @throws IllegalArgumentException if no component was added
     */
    /**
     * Sets how repeated observations combine. Omit the call for a plain counter.
     *
     * @throws NullPointerException if {@code aggregation} is {@code null}
     */
    public Builder aggregation(RecordAggregation aggregation) {
      this.aggregation = Objects.requireNonNull(aggregation, "aggregation must not be null");
      return this;
    }

    public RecordDefinition build() {
      return new RecordDefinition(
          RecordKey.of(components.values()), name, description, aggregation, components);
    }

    private static String requireText(String text, String what) {
      Objects.requireNonNull(text, what + " must not be null");
      if (text.isBlank()) {
        throw new IllegalArgumentException(what + " must not be blank");
      }
      return text;
    }
  }
}
