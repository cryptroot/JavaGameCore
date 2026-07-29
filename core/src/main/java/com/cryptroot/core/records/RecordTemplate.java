package com.cryptroot.core.records;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * A family of records differing in exactly one component: fixed components plus one open {@link
 * #parameter()} dimension whose value is supplied per member.
 *
 * <p>This is how a game counts something it cannot enumerate at start-up — one record per skill,
 * weapon, quest, or enemy type, where the ids come from content data:
 *
 * <pre>{@code
 * RecordTemplate weaponKills = RecordTemplate.builder()
 *     .components(EVENT_KILL, MEASURE_TIMES)
 *     .parameter(RecordDimension.of("starfarer", "weapon"))
 *     .name(weapon -> "Kills with " + weapon)
 *     .build();
 *
 * RecordKey railgun = weaponKills.define(keeper, "railgun");
 * book.record(railgun);
 * }</pre>
 *
 * <p>A member is an ordinary {@link RecordDefinition}, so unlike a hand-rolled {@code "skill_" +
 * id} key it participates in every component query, in {@link RecordBookComponent#reset}, and in
 * {@link RecordKeeper#getName} — the parameter is just another component. {@link #family()} is the
 * query matching every member of the family and nothing else.
 *
 * <p>Immutable and reusable: build one during start-up and call {@link #define} whenever a new
 * member is first touched. Registration is idempotent, so callers need not track what they have
 * already defined.
 */
public final class RecordTemplate {

  private final SortedMap<RecordDimension, RecordComponent> fixed;
  private final RecordDimension parameter;
  private final Function<String, String> naming;
  private final Function<String, String> describing;
  private final RecordAggregation aggregation;

  private RecordTemplate(Builder builder) {
    this.fixed = Collections.unmodifiableSortedMap(new TreeMap<>(builder.fixed));
    this.parameter = builder.parameter;
    this.naming = builder.naming;
    this.describing = builder.describing;
    this.aggregation = builder.aggregation;
  }

  /** A fresh builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** The dimension whose value distinguishes one member of this family from another. */
  public RecordDimension parameter() {
    return parameter;
  }

  /** The fixed components every member of this family carries, in dimension order. */
  public SortedMap<RecordDimension, RecordComponent> components() {
    return fixed;
  }

  /**
   * The key of the member for {@code parameterValue}, without registering anything.
   *
   * @throws IllegalArgumentException if {@code parameterValue} is not canonical
   */
  public RecordKey key(String parameterValue) {
    return definition(parameterValue).key();
  }

  /**
   * The key of the member for a numeric id — sugar for content that identifies members by number.
   *
   * @throws IllegalArgumentException if {@code parameterValue} is negative
   */
  public RecordKey key(int parameterValue) {
    return key(requireNonNegative(parameterValue));
  }

  /**
   * The definition of the member for {@code parameterValue}, with its name and description filled
   * in from this template's naming functions.
   *
   * @throws NullPointerException if {@code parameterValue} is {@code null}
   * @throws IllegalArgumentException if {@code parameterValue} is not canonical
   */
  public RecordDefinition definition(String parameterValue) {
    Objects.requireNonNull(parameterValue, "parameterValue must not be null");
    RecordDefinition.Builder builder = RecordDefinition.builder().aggregation(aggregation);
    fixed.values().forEach(builder::component);
    builder.component(RecordComponent.of(parameter, parameterValue));
    if (naming != null) {
      builder.name(naming.apply(parameterValue));
    }
    if (describing != null) {
      builder.description(describing.apply(parameterValue));
    }
    return builder.build();
  }

  /**
   * Registers the member for {@code parameterValue} into {@code keeper} if it is not already there,
   * and returns its key.
   *
   * <p>Idempotent by design: members are discovered as play happens, so the first railgun kill and
   * the thousandth both call this without the caller remembering which came first. Contrast {@link
   * RecordKeeper#define}, which rejects a duplicate because a hand-written catalogue defining the
   * same record twice is a content bug.
   *
   * @throws NullPointerException if either argument is {@code null}
   */
  public RecordKey define(RecordKeeper keeper, String parameterValue) {
    Objects.requireNonNull(keeper, "keeper must not be null");
    return keeper.defineIfAbsent(definition(parameterValue));
  }

  /**
   * Registers the member for a numeric id.
   *
   * @throws IllegalArgumentException if {@code parameterValue} is negative
   */
  public RecordKey define(RecordKeeper keeper, int parameterValue) {
    return define(keeper, requireNonNegative(parameterValue));
  }

  /**
   * The query matching every member of this family — every record carrying all the fixed components
   * and some value on the parameter dimension.
   */
  public RecordQuery family() {
    RecordQuery query = RecordQuery.hasDimension(parameter);
    for (RecordComponent component : fixed.values()) {
      query = query.and(RecordQuery.has(component));
    }
    return query;
  }

  private static String requireNonNegative(int parameterValue) {
    if (parameterValue < 0) {
      throw new IllegalArgumentException("parameterValue must not be negative: " + parameterValue);
    }
    return Integer.toString(parameterValue);
  }

  /** Fluent builder for a {@link RecordTemplate}. Not thread-safe; use one per template. */
  public static final class Builder {

    private final SortedMap<RecordDimension, RecordComponent> fixed = new TreeMap<>();
    private RecordDimension parameter;
    private Function<String, String> naming;
    private Function<String, String> describing;
    private RecordAggregation aggregation = RecordAggregation.SUM;

    private Builder() {}

    /**
     * Adds one component every member of the family carries.
     *
     * @throws IllegalArgumentException if a component was already added for the same dimension
     */
    public Builder component(RecordComponent component) {
      Objects.requireNonNull(component, "component must not be null");
      RecordComponent canonical = RecordComponent.canonical(component);
      RecordComponent existing = fixed.putIfAbsent(canonical.dimension(), canonical);
      if (existing != null) {
        throw new IllegalArgumentException(
            "dimension "
                + canonical.dimension()
                + " already set to "
                + existing.value()
                + ", cannot also be "
                + canonical.value());
      }
      return this;
    }

    /** Adds several fixed components, in any order. */
    public Builder components(RecordComponent... toAdd) {
      Objects.requireNonNull(toAdd, "toAdd must not be null");
      for (RecordComponent component : toAdd) {
        component(component);
      }
      return this;
    }

    /**
     * Sets the dimension carrying each member's distinguishing value. Required.
     *
     * @throws IllegalArgumentException if the parameter dimension is already fixed
     */
    public Builder parameter(RecordDimension parameter) {
      this.parameter = Objects.requireNonNull(parameter, "parameter must not be null");
      return this;
    }

    /**
     * Derives each member's display name from its parameter value. Omit to leave members unnamed.
     */
    public Builder name(Function<String, String> naming) {
      this.naming = Objects.requireNonNull(naming, "naming must not be null");
      return this;
    }

    /**
     * Derives each member's description from its parameter value. Omit to leave them undescribed.
     */
    public Builder description(Function<String, String> describing) {
      this.describing = Objects.requireNonNull(describing, "describing must not be null");
      return this;
    }

    /**
     * Sets how repeated observations combine for every member. Omit for plain counters.
     *
     * @throws NullPointerException if {@code aggregation} is {@code null}
     */
    public Builder aggregation(RecordAggregation aggregation) {
      this.aggregation = Objects.requireNonNull(aggregation, "aggregation must not be null");
      return this;
    }

    /**
     * Builds the template.
     *
     * @throws IllegalStateException if no {@link #parameter} was set
     * @throws IllegalArgumentException if the parameter dimension is also one of the fixed
     *     components
     */
    public RecordTemplate build() {
      if (parameter == null) {
        throw new IllegalStateException("a template needs a parameter dimension");
      }
      if (fixed.containsKey(parameter)) {
        throw new IllegalArgumentException(
            "parameter dimension "
                + parameter
                + " is already fixed to "
                + fixed.get(parameter).value());
      }
      return new RecordTemplate(this);
    }
  }
}
