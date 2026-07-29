package com.cryptroot.core.records;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One axis a record can be classified along, identified by a canonical {@link #id()}.
 *
 * <p>A record carries at most one {@link RecordComponent} per dimension — "blood, donated by the
 * player, at the bank, measured in times" is four components on four different dimensions. The
 * dimension is part of a generated key (see {@link RecordKey#of}), so the same value on two
 * different axes can never collide.
 *
 * <p>This is a plain value type, not an enum: a game declares whatever axes it needs without
 * editing this module, and two dimensions with the same {@code id} are equal wherever they were
 * built. Library authors sharing records across games should namespace their axes — {@link
 * #of(String, String) of("starfarer", "ship")} yields the id {@code "starfarer:ship"} — but a
 * single game is free to use bare ids.
 *
 * <p>Deliberately not interned: there is no registry to keep in sync, no cross-session leakage, and
 * no test ordering to worry about. Compare with {@code equals}, never {@code ==}.
 *
 * @param id the canonical identifier, matching {@code [a-z0-9][a-z0-9_.:-]*}
 */
public record RecordDimension(String id) implements Comparable<RecordDimension> {

  /** The shape every {@code id} — and every {@link RecordComponent#value()} — must have. */
  static final Pattern CANONICAL = Pattern.compile("[a-z0-9][a-z0-9_.:-]*");

  /** The shape of one namespace or id segment: {@link #CANONICAL} without the separator. */
  private static final Pattern SEGMENT = Pattern.compile("[a-z0-9][a-z0-9_.-]*");

  /** Separates a namespace from an id. */
  public static final String NAMESPACE_SEPARATOR = ":";

  /**
   * Validates {@code id}.
   *
   * <p>Fail-fast rather than normalising: an id is rejected outright when it is blank, uppercase or
   * carries an illegal character, so {@link #id()} always returns exactly what the caller passed.
   * Silently lowercasing would make {@code of("Ship").id()} surprising and would let {@code "ship
   * "} through. {@code =} and {@code |} are excluded because {@link RecordKey} joins with them.
   *
   * @throws NullPointerException if {@code id} is {@code null}
   * @throws IllegalArgumentException if {@code id} is not canonical
   */
  public RecordDimension {
    requireCanonical(id, "dimension id");
  }

  /** A dimension with the given canonical id. */
  public static RecordDimension of(String id) {
    return new RecordDimension(id);
  }

  /**
   * A dimension identified as {@code namespace:id} — the convention for axes shared between games.
   *
   * @throws IllegalArgumentException if either part is blank, uppercase, or contains a separator
   */
  public static RecordDimension of(String namespace, String id) {
    requireSegment(namespace, "namespace");
    requireSegment(id, "dimension id");
    return new RecordDimension(namespace + NAMESPACE_SEPARATOR + id);
  }

  /**
   * Orders by {@link #id()}, so a {@code SortedMap} keyed by dimension iterates deterministically.
   */
  @Override
  public int compareTo(RecordDimension other) {
    Objects.requireNonNull(other, "other must not be null");
    return id.compareTo(other.id);
  }

  /** The {@link #id()} — dimensions read as their id everywhere, including in error messages. */
  @Override
  public String toString() {
    return id;
  }

  /**
   * Validates one canonical token, shared with {@link RecordComponent}.
   *
   * @throws NullPointerException if {@code token} is {@code null}
   * @throws IllegalArgumentException if {@code token} does not match {@link #CANONICAL}
   */
  static String requireCanonical(String token, String what) {
    Objects.requireNonNull(token, what + " must not be null");
    if (!CANONICAL.matcher(token).matches()) {
      throw new IllegalArgumentException(
          what + " must match " + CANONICAL.pattern() + " but was \"" + token + "\"");
    }
    return token;
  }

  private static void requireSegment(String segment, String what) {
    Objects.requireNonNull(segment, what + " must not be null");
    if (!SEGMENT.matcher(segment).matches()) {
      throw new IllegalArgumentException(
          what + " must match " + SEGMENT.pattern() + " but was \"" + segment + "\"");
    }
  }
}
