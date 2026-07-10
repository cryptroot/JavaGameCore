package com.cryptroot.tiled.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

/**
 * A single custom property (a {@code <property>} element).
 *
 * <p>A property value is normally stored in the {@code value} attribute. When a string value
 * contains newlines, Tiled instead writes the value as the element's character content; both forms
 * are accepted here and exposed uniformly through {@link #value()}.
 *
 * <p>This is a parsed, read-only data holder: fields are populated by Jackson and exposed only
 * through accessors.
 */
public final class Property {

  @JacksonXmlProperty(isAttribute = true)
  private String name;

  @JacksonXmlProperty(isAttribute = true)
  private String type;

  @JacksonXmlProperty(isAttribute = true)
  private String value;

  @JacksonXmlText private String text;

  /**
   * @return the property name, or {@code null} if absent.
   */
  public String name() {
    return name;
  }

  /**
   * @return the declared type, defaulting to {@link PropertyType#STRING}.
   */
  public PropertyType type() {
    return PropertyType.fromTmx(type);
  }

  /**
   * @return the raw string value from either the {@code value} attribute or the element's character
   *     content, or an empty string if neither is present.
   */
  public String value() {
    if (value != null) {
      return value;
    }
    return text != null ? text : "";
  }

  /**
   * @return the value as a string (alias for {@link #value()}).
   */
  public String asString() {
    return value();
  }

  /**
   * @return the value parsed as an {@code int}.
   */
  public int asInt() {
    return Integer.parseInt(value().trim());
  }

  /**
   * @return the value parsed as a {@code long}.
   */
  public long asLong() {
    return Long.parseLong(value().trim());
  }

  /**
   * @return the value parsed as a {@code float}.
   */
  public float asFloat() {
    return Float.parseFloat(value().trim());
  }

  /**
   * @return {@code true} when the value is {@code "true"} (case-insensitive) or {@code "1"}; {@code
   *     false} otherwise.
   */
  public boolean asBool() {
    String v = value().trim();
    return "true".equalsIgnoreCase(v) || "1".equals(v);
  }
}
