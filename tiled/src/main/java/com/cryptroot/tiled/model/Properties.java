package com.cryptroot.tiled.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A bag of custom {@link Property} entries (a {@code <properties>} element).
 *
 * <p>Attached to maps, tilesets, tiles, layers, object groups and objects. Lookup is by property
 * name; convenience accessors fall back to a supplied default when a name is absent.
 */
public final class Properties {

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "property")
  private List<Property> property = new ArrayList<>();

  /**
   * @return an unmodifiable view of all properties, in document order.
   */
  public List<Property> all() {
    return Collections.unmodifiableList(property);
  }

  /**
   * @return {@code true} if a property with the given name exists.
   */
  public boolean has(String name) {
    return find(name).isPresent();
  }

  /**
   * @return the first property with the given name, if any.
   */
  public Optional<Property> find(String name) {
    for (Property p : property) {
      if (name != null && name.equals(p.name())) {
        return Optional.of(p);
      }
    }
    return Optional.empty();
  }

  /**
   * @return the string value for {@code name}, or {@code def} if absent.
   */
  public String getString(String name, String def) {
    return find(name).map(Property::asString).orElse(def);
  }

  /**
   * @return the int value for {@code name}, or {@code def} if absent.
   */
  public int getInt(String name, int def) {
    return find(name).map(Property::asInt).orElse(def);
  }

  /**
   * @return the float value for {@code name}, or {@code def} if absent.
   */
  public float getFloat(String name, float def) {
    return find(name).map(Property::asFloat).orElse(def);
  }

  /**
   * @return the boolean value for {@code name}, or {@code def} if absent.
   */
  public boolean getBool(String name, boolean def) {
    return find(name).map(Property::asBool).orElse(def);
  }
}
