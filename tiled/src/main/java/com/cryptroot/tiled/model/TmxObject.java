package com.cryptroot.tiled.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A single object within an {@link ObjectGroup} (an {@code <object>} element).
 *
 * <p>Objects are positioned in pixels. An object may be a plain rectangle, an {@link Shape#ELLIPSE
 * ellipse}, a {@link Shape#POINT point}, a {@link Shape#POLYGON polygon}, a {@link Shape#POLYLINE
 * polyline}, or a {@link Shape#TILE tile object} (one referencing a tile via {@link #gid()}).
 *
 * <p>Only tile objects are rendered by this library; other shapes are exposed as data for consumers
 * to interpret (spawn points, triggers, collision regions, …).
 *
 * <p>This is a parsed, read-only data holder.
 */
public final class TmxObject {

  /** The geometric kind of an object. */
  public enum Shape {
    /** An axis-aligned rectangle defined by x/y/width/height. */
    RECTANGLE,
    /** An ellipse inscribed in the x/y/width/height bounds. */
    ELLIPSE,
    /** A single point at x/y. */
    POINT,
    /** A closed polygon relative to x/y. */
    POLYGON,
    /** An open polyline relative to x/y. */
    POLYLINE,
    /** A tile image placed at x/y, referenced by {@link TmxObject#gid()}. */
    TILE
  }

  /** Presence marker for empty child elements such as {@code <point/>} and {@code <ellipse/>}. */
  public static final class Marker {}

  /** Holder for a {@code points} attribute of a {@code <polygon>}/{@code <polyline>}. */
  public static final class Poly {
    @JacksonXmlProperty(isAttribute = true)
    private String points;

    /**
     * @return the raw {@code points} attribute (space-separated {@code x,y} pairs).
     */
    public String points() {
      return points;
    }
  }

  @JacksonXmlProperty(isAttribute = true)
  private int id;

  @JacksonXmlProperty(isAttribute = true)
  private String name;

  @JacksonXmlProperty(isAttribute = true, localName = "type")
  private String type;

  @JacksonXmlProperty(isAttribute = true)
  private double x;

  @JacksonXmlProperty(isAttribute = true)
  private double y;

  @JacksonXmlProperty(isAttribute = true)
  private double width;

  @JacksonXmlProperty(isAttribute = true)
  private double height;

  @JacksonXmlProperty(isAttribute = true)
  private double rotation;

  @JacksonXmlProperty(isAttribute = true)
  private Long gid;

  @JacksonXmlProperty(isAttribute = true)
  private Integer visible;

  @JacksonXmlProperty(localName = "properties")
  private Properties properties;

  @JacksonXmlProperty(localName = "point")
  private Marker point;

  @JacksonXmlProperty(localName = "ellipse")
  private Marker ellipse;

  @JacksonXmlProperty(localName = "polygon")
  private Poly polygon;

  @JacksonXmlProperty(localName = "polyline")
  private Poly polyline;

  /**
   * @return the object's unique id, or {@code 0} when unset.
   */
  public int id() {
    return id;
  }

  /**
   * @return the object name, or {@code null} when unset.
   */
  public String name() {
    return name;
  }

  /**
   * @return the object's class/type, or {@code null} when unset.
   */
  public String type() {
    return type;
  }

  /**
   * @return the object's x coordinate in pixels.
   */
  public double x() {
    return x;
  }

  /**
   * @return the object's y coordinate in pixels (top-down; y increases downward).
   */
  public double y() {
    return y;
  }

  /**
   * @return the object's width in pixels.
   */
  public double width() {
    return width;
  }

  /**
   * @return the object's height in pixels.
   */
  public double height() {
    return height;
  }

  /**
   * @return the object's clockwise rotation in degrees, defaulting to {@code 0}.
   */
  public double rotation() {
    return rotation;
  }

  /**
   * @return the raw global tile id (including flip flags) when this is a tile object, otherwise
   *     empty.
   */
  public OptionalLong gid() {
    return gid != null ? OptionalLong.of(gid) : OptionalLong.empty();
  }

  /**
   * @return whether the object is shown; defaults to {@code true}.
   */
  public boolean visible() {
    return visible == null || visible != 0;
  }

  /**
   * @return the object's custom properties, if any.
   */
  public Optional<Properties> properties() {
    return Optional.ofNullable(properties);
  }

  /**
   * @return the raw {@code points} string for a polygon or polyline object, or {@code null} for
   *     other shapes.
   */
  public String points() {
    if (polygon != null) {
      return polygon.points();
    }
    return polyline != null ? polyline.points() : null;
  }

  /**
   * @return the geometric kind of this object.
   */
  public Shape shape() {
    if (gid != null) {
      return Shape.TILE;
    }
    if (point != null) {
      return Shape.POINT;
    }
    if (ellipse != null) {
      return Shape.ELLIPSE;
    }
    if (polygon != null) {
      return Shape.POLYGON;
    }
    if (polyline != null) {
      return Shape.POLYLINE;
    }
    return Shape.RECTANGLE;
  }
}
