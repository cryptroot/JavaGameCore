package com.cryptroot.tiled.model;

/**
 * The declared type of a custom {@link Property}, as given by the {@code type} attribute of a
 * {@code <property>} element.
 */
public enum PropertyType {

  /** A plain string (the TMX default when {@code type} is omitted). */
  STRING,

  /** A signed integer. */
  INT,

  /** A floating-point number. */
  FLOAT,

  /** A boolean, stored as {@code "true"} or {@code "false"}. */
  BOOL,

  /** A colour, stored in {@code #AARRGGBB} format. */
  COLOR,

  /** A file path, stored relative to the map file. */
  FILE,

  /** A reference to an object, stored as the referenced object's id. */
  OBJECT,

  /** A structured value whose members are stored in a nested {@code <properties>}. */
  CLASS;

  /**
   * Maps a raw TMX {@code type} attribute value to a constant.
   *
   * @param value the attribute value (e.g. {@code "int"}); may be {@code null}
   * @return the matching constant, defaulting to {@link #STRING}
   */
  public static PropertyType fromTmx(String value) {
    if (value == null) {
      return STRING;
    }
    return switch (value.trim().toLowerCase()) {
      case "int" -> INT;
      case "float" -> FLOAT;
      case "bool" -> BOOL;
      case "color" -> COLOR;
      case "file" -> FILE;
      case "object" -> OBJECT;
      case "class" -> CLASS;
      case "string" -> STRING;
      default -> STRING;
    };
  }
}
