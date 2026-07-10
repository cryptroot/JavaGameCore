package com.cryptroot.tiled.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A layer of freely-placed objects (an {@code <objectgroup>} element).
 *
 * <p>Objects carry pixel coordinates rather than grid coordinates and are commonly used for spawn
 * points, triggers, collision regions and tile objects.
 *
 * <p>This is a parsed, read-only data holder.
 */
public final class ObjectGroup extends TmxLayer {

  @JacksonXmlProperty(isAttribute = true)
  private String draworder;

  @JacksonXmlProperty(isAttribute = true)
  private String color;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "object")
  private List<TmxObject> object = new ArrayList<>();

  /**
   * @return the draw order for objects in this group, defaulting to {@link DrawOrder#TOPDOWN}.
   */
  public DrawOrder drawOrder() {
    return DrawOrder.fromTmx(draworder);
  }

  /**
   * @return the colour used to display the objects in this group in {@code #AARRGGBB} or {@code
   *     #RRGGBB} form, or {@code null} when unset.
   */
  public String color() {
    return color;
  }

  /**
   * @return an unmodifiable view of the objects in this group, in document order.
   */
  public List<TmxObject> objects() {
    return Collections.unmodifiableList(object);
  }
}
