package com.cryptroot.tiled.model;

/**
 * Map orientation as declared by the {@code orientation} attribute of the {@code <map>} element.
 *
 * <p>The tiled library currently only <em>renders</em> {@link #ORTHOGONAL} maps. The remaining
 * constants exist so the parser can faithfully report the orientation of an unsupported map instead
 * of failing to parse it.
 */
public enum Orientation {

  /** Rectangular, axis-aligned tiles. The only orientation the renderer supports. */
  ORTHOGONAL,

  /** Diamond-shaped isometric tiles. Parsed but not rendered. */
  ISOMETRIC,

  /** Staggered isometric tiles. Parsed but not rendered. */
  STAGGERED,

  /** Hexagonal tiles. Parsed but not rendered. */
  HEXAGONAL;

  /**
   * Maps a raw TMX {@code orientation} attribute value to a constant.
   *
   * @param value the attribute value (e.g. {@code "orthogonal"}); may be {@code null}
   * @return the matching constant, defaulting to {@link #ORTHOGONAL} when {@code null}
   * @throws IllegalArgumentException if {@code value} is a non-null, unrecognised string
   */
  public static Orientation fromTmx(String value) {
    if (value == null) {
      return ORTHOGONAL;
    }
    return switch (value.trim().toLowerCase()) {
      case "orthogonal" -> ORTHOGONAL;
      case "isometric" -> ISOMETRIC;
      case "staggered" -> STAGGERED;
      case "hexagonal" -> HEXAGONAL;
      default -> throw new IllegalArgumentException("Unknown map orientation: " + value);
    };
  }
}
