package com.cryptroot.core.records;

import java.util.Objects;

/**
 * One classifying value of a record, on exactly one {@link RecordDimension}.
 *
 * <p>Two ways to supply components, both first-class:
 *
 * <ul>
 *   <li>{@link #of(RecordDimension, String)} — no declaration needed, ideal for data-driven
 *       content.
 *   <li>a game-owned enum implementing this interface, for completion and exhaustive switches:
 *       <pre>{@code
 * enum Ship implements RecordComponent {
 *   INTERCEPTOR("interceptor"),
 *   FREIGHTER("freighter");
 *
 *   static final RecordDimension DIMENSION = RecordDimension.of("starfarer", "ship");
 *
 *   private final String value;
 *
 *   Ship(String value) {
 *     this.value = value;
 *   }
 *
 *   public RecordDimension dimension() {
 *     return DIMENSION;
 *   }
 *
 *   public String value() {
 *     return value;
 *   }
 * }
 * }</pre>
 * </ul>
 *
 * <p>Implementations need <em>not</em> implement {@link Object#equals}: this module canonicalises
 * every component it is handed (see {@link #canonical}) and compares by {@code (dimension, value)}
 * alone. So an enum constant and an {@link #of} component naming the same axis and value are
 * interchangeable in every query, across games that never share a type.
 *
 * <p>A record's identity is the set of its components; {@link RecordKey#of} renders them into a
 * stable key ordered by dimension, so supply order never matters.
 */
public interface RecordComponent {

  /** The axis this component classifies a record along. Never {@code null}. */
  RecordDimension dimension();

  /**
   * The canonical token this component contributes to a generated key, matching {@code
   * [a-z0-9][a-z0-9_.:-]*}. Never {@code null} or blank.
   *
   * <p>Declare it explicitly rather than deriving it from an enum's {@code name()}, so renaming a
   * constant can never silently change an already-persisted record key.
   */
  String value();

  /**
   * A component on {@code dimension} with {@code value}, needing no declaration.
   *
   * @throws NullPointerException if either argument is {@code null}
   * @throws IllegalArgumentException if {@code value} is not canonical
   */
  static RecordComponent of(RecordDimension dimension, String value) {
    return new Basic(dimension, value);
  }

  /**
   * {@code component} as a {@link Basic}, so equality is by {@code (dimension, value)}.
   *
   * <p>Returns {@code component} itself when it is already a {@code Basic}. Every entry point that
   * stores or compares a component runs it through here; that is what lets an enum constant and an
   * {@link #of} component be equal despite an enum never being able to {@code equals} a record.
   *
   * @throws NullPointerException if {@code component} is {@code null}
   * @throws IllegalArgumentException if {@code component} reports a non-canonical value
   */
  static RecordComponent canonical(RecordComponent component) {
    Objects.requireNonNull(component, "component must not be null");
    if (component instanceof Basic basic) {
      return basic;
    }
    return new Basic(component.dimension(), component.value());
  }

  /**
   * The canonical implementation: a bare {@code (dimension, value)} pair.
   *
   * @param dimension the axis, never {@code null}
   * @param value the canonical token, never {@code null} or blank
   */
  record Basic(RecordDimension dimension, String value) implements RecordComponent {

    /**
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if {@code value} is not canonical
     */
    public Basic {
      Objects.requireNonNull(dimension, "dimension must not be null");
      RecordDimension.requireCanonical(value, "component value");
    }

    /** Renders as {@code dimension=value}, matching how the component appears in a key. */
    @Override
    public String toString() {
      return dimension.id() + "=" + value;
    }
  }
}
