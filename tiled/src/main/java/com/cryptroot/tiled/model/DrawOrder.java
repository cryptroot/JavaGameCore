package com.cryptroot.tiled.model;

/**
 * Determines the order in which objects in an {@link ObjectGroup} are drawn, as declared by the
 * {@code draworder} attribute of the {@code <objectgroup>} element.
 */
public enum DrawOrder {

  /** Objects are drawn sorted by their y-coordinate (the TMX default). */
  TOPDOWN,

  /** Objects are drawn in their order of appearance in the file. */
  INDEX;

  /**
   * Maps a raw TMX {@code draworder} attribute value to a constant.
   *
   * @param value the attribute value (e.g. {@code "topdown"}); may be {@code null}
   * @return the matching constant, defaulting to {@link #TOPDOWN}
   */
  public static DrawOrder fromTmx(String value) {
    if (value == null) {
      return TOPDOWN;
    }
    return switch (value.trim().toLowerCase()) {
      case "index" -> INDEX;
      case "topdown" -> TOPDOWN;
      default -> TOPDOWN;
    };
  }
}
