package com.cryptroot.tiled.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Optional;

/**
 * Common state shared by all map layers.
 *
 * <p>This is a sealed hierarchy: a layer is either a {@link TileLayer} (a grid of tiles) or an
 * {@link ObjectGroup} (freely-placed objects). Image layers and group layers are out of scope and
 * are not modelled.
 *
 * <p>Consumers can dispatch over the concrete kind with a pattern-matching {@code switch}:
 *
 * <pre>{@code
 * switch (layer) {
 *     case TileLayer tl   -> ...;
 *     case ObjectGroup og -> ...;
 * }
 * }</pre>
 *
 * <p>This is a parsed, read-only data holder.
 */
public abstract sealed class TmxLayer permits TileLayer, ObjectGroup {

  @JacksonXmlProperty(isAttribute = true)
  protected int id;

  @JacksonXmlProperty(isAttribute = true)
  protected String name;

  @JacksonXmlProperty(isAttribute = true, localName = "class")
  protected String clazz;

  @JacksonXmlProperty(isAttribute = true)
  protected Double opacity;

  @JacksonXmlProperty(isAttribute = true)
  protected Integer visible;

  @JacksonXmlProperty(isAttribute = true)
  protected Double offsetx;

  @JacksonXmlProperty(isAttribute = true)
  protected Double offsety;

  @JacksonXmlProperty(isAttribute = true)
  protected String tintcolor;

  @JacksonXmlProperty(localName = "properties")
  protected Properties properties;

  /**
   * @return the layer's unique id, or {@code 0} when unset.
   */
  public int id() {
    return id;
  }

  /**
   * @return the layer name, defaulting to an empty string.
   */
  public String name() {
    return name != null ? name : "";
  }

  /**
   * @return the layer's class/type, or {@code null} when unset.
   */
  public String type() {
    return clazz;
  }

  /**
   * @return the layer opacity in {@code [0, 1]}, defaulting to {@code 1}.
   */
  public float opacity() {
    return opacity != null ? opacity.floatValue() : 1f;
  }

  /**
   * @return whether the layer is shown; defaults to {@code true}.
   */
  public boolean visible() {
    return visible == null || visible != 0;
  }

  /**
   * @return the horizontal layer offset in pixels, defaulting to {@code 0}.
   */
  public float offsetX() {
    return offsetx != null ? offsetx.floatValue() : 0f;
  }

  /**
   * @return the vertical layer offset in pixels (positive is down), defaulting to {@code 0}.
   */
  public float offsetY() {
    return offsety != null ? offsety.floatValue() : 0f;
  }

  /**
   * @return the layer tint colour in {@code #AARRGGBB} or {@code #RRGGBB} form, or {@code null}
   *     when no tint is applied.
   */
  public String tintColor() {
    return tintcolor;
  }

  /**
   * @return the layer's custom properties, if any.
   */
  public Optional<Properties> properties() {
    return Optional.ofNullable(properties);
  }
}
