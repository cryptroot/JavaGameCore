package com.cryptroot.tiled.model;

/**
 * The order in which tiles on a tile layer are rendered, as declared by the {@code renderorder}
 * attribute of the {@code <map>} element. In all cases the map is drawn row-by-row.
 *
 * <p>Only supported for orthogonal maps. Defaults to {@link #RIGHT_DOWN}.
 */
public enum RenderOrder {

  /** Left-to-right, top-to-bottom (the TMX default). */
  RIGHT_DOWN,

  /** Left-to-right, bottom-to-top. */
  RIGHT_UP,

  /** Right-to-left, top-to-bottom. */
  LEFT_DOWN,

  /** Right-to-left, bottom-to-top. */
  LEFT_UP;

  /**
   * Maps a raw TMX {@code renderorder} attribute value to a constant.
   *
   * @param value the attribute value (e.g. {@code "right-down"}); may be {@code null}
   * @return the matching constant, defaulting to {@link #RIGHT_DOWN}
   */
  public static RenderOrder fromTmx(String value) {
    if (value == null) {
      return RIGHT_DOWN;
    }
    return switch (value.trim().toLowerCase()) {
      case "right-down" -> RIGHT_DOWN;
      case "right-up" -> RIGHT_UP;
      case "left-down" -> LEFT_DOWN;
      case "left-up" -> LEFT_UP;
      default -> RIGHT_DOWN;
    };
  }
}
