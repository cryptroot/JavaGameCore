package com.cryptroot.core.records;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A predicate over {@link RecordDefinition}s, composable with AND, OR and NOT.
 *
 * <pre>{@code
 * // every kill, however it was scored
 * RecordQuery.has(KILL)
 *
 * // kills that were not scored by a freighter
 * RecordQuery.has(KILL).and(RecordQuery.has(Ship.FREIGHTER).negate())
 *
 * // anything measured at all, however it is measured
 * RecordQuery.hasDimension(MEASURE)
 * }</pre>
 *
 * <p>Sealed, so the shapes are exhaustive and inspectable — except for {@link Where}, the escape
 * hatch for conditions no combinator covers. Queries are matching logic, never persisted, so an
 * opaque predicate costs nothing here.
 */
public sealed interface RecordQuery {

  /** {@code true} if {@code definition} satisfies this query. */
  boolean test(RecordDefinition definition);

  /** Matches every definition — the identity of {@link #allOf}. */
  static RecordQuery anything() {
    return new All(List.of());
  }

  /** Matches no definition — the identity of {@link #anyOf}. */
  static RecordQuery nothing() {
    return new Any(List.of());
  }

  /** Matches definitions carrying {@code component}, compared by {@code (dimension, value)}. */
  static RecordQuery has(RecordComponent component) {
    return new Has(component);
  }

  /** Matches definitions carrying <em>some</em> component on {@code dimension}. */
  static RecordQuery hasDimension(RecordDimension dimension) {
    return new HasDimension(dimension);
  }

  /** Matches definitions satisfying every one of {@code queries}. */
  static RecordQuery allOf(RecordQuery... queries) {
    return new All(List.of(queries));
  }

  /** Matches definitions satisfying at least one of {@code queries}. */
  static RecordQuery anyOf(RecordQuery... queries) {
    return new Any(List.of(queries));
  }

  /** Matches definitions {@code query} does not match. */
  static RecordQuery not(RecordQuery query) {
    return new Not(query);
  }

  /** Matches definitions satisfying {@code predicate} — the escape hatch. */
  static RecordQuery where(Predicate<RecordDefinition> predicate) {
    return new Where(predicate);
  }

  /** This query and {@code other}. */
  default RecordQuery and(RecordQuery other) {
    return allOf(this, other);
  }

  /** This query or {@code other}. */
  default RecordQuery or(RecordQuery other) {
    return anyOf(this, other);
  }

  /** The negation of this query. */
  default RecordQuery negate() {
    return not(this);
  }

  /**
   * Carries one exact component.
   *
   * @param component the component every match must carry, never {@code null}
   */
  record Has(RecordComponent component) implements RecordQuery {

    /**
     * @throws NullPointerException if {@code component} is {@code null}
     * @throws IllegalArgumentException if {@code component} reports a non-canonical value
     */
    public Has {
      component = RecordComponent.canonical(component);
    }

    @Override
    public boolean test(RecordDefinition definition) {
      Objects.requireNonNull(definition, "definition must not be null");
      return definition.has(component);
    }
  }

  /**
   * Carries some value on one dimension.
   *
   * @param dimension the axis every match must be classified on, never {@code null}
   */
  record HasDimension(RecordDimension dimension) implements RecordQuery {

    /**
     * @throws NullPointerException if {@code dimension} is {@code null}
     */
    public HasDimension {
      Objects.requireNonNull(dimension, "dimension must not be null");
    }

    @Override
    public boolean test(RecordDefinition definition) {
      Objects.requireNonNull(definition, "definition must not be null");
      return definition.component(dimension).isPresent();
    }
  }

  /**
   * Conjunction. An empty {@code queries} matches everything.
   *
   * @param queries the queries every match must satisfy, never {@code null}, unmodifiable
   */
  record All(List<RecordQuery> queries) implements RecordQuery {

    /**
     * @throws NullPointerException if {@code queries} or any element is {@code null}
     */
    public All {
      queries = List.copyOf(queries);
    }

    @Override
    public boolean test(RecordDefinition definition) {
      for (RecordQuery query : queries) {
        if (!query.test(definition)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Disjunction. An empty {@code queries} matches nothing.
   *
   * @param queries the queries at least one of which a match must satisfy, unmodifiable
   */
  record Any(List<RecordQuery> queries) implements RecordQuery {

    /**
     * @throws NullPointerException if {@code queries} or any element is {@code null}
     */
    public Any {
      queries = List.copyOf(queries);
    }

    @Override
    public boolean test(RecordDefinition definition) {
      for (RecordQuery query : queries) {
        if (query.test(definition)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Negation.
   *
   * @param query the query a match must fail, never {@code null}
   */
  record Not(RecordQuery query) implements RecordQuery {

    /**
     * @throws NullPointerException if {@code query} is {@code null}
     */
    public Not {
      Objects.requireNonNull(query, "query must not be null");
    }

    @Override
    public boolean test(RecordDefinition definition) {
      return !query.test(definition);
    }
  }

  /**
   * An arbitrary predicate, for conditions the combinators cannot express.
   *
   * @param predicate the condition a match must satisfy, never {@code null}
   */
  record Where(Predicate<RecordDefinition> predicate) implements RecordQuery {

    /**
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
    public Where {
      Objects.requireNonNull(predicate, "predicate must not be null");
    }

    @Override
    public boolean test(RecordDefinition definition) {
      Objects.requireNonNull(definition, "definition must not be null");
      return predicate.test(definition);
    }
  }
}
