package com.cryptroot.tiled.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Optional;

/**
 * A per-tile definition within a {@link TmxTileset} (a {@code <tile>} element nested inside a
 * {@code <tileset>}).
 *
 * <p>Two uses are supported:
 *
 * <ul>
 *   <li>In an image-collection tileset, each tile carries its own {@link #image()}.
 *   <li>In any tileset, a tile may declare a {@code type}/class and custom {@link #properties()}.
 * </ul>
 *
 * <p>Tile animations, collision object groups and terrain/wang data are out of scope and ignored.
 *
 * <p>This is a parsed, read-only data holder.
 */
public final class TmxTile {

  @JacksonXmlProperty(isAttribute = true)
  private int id;

  @JacksonXmlProperty(isAttribute = true, localName = "type")
  private String type;

  @JacksonXmlProperty(isAttribute = true)
  private Double probability;

  @JacksonXmlProperty(localName = "image")
  private TmxImage image;

  @JacksonXmlProperty(localName = "properties")
  private Properties properties;

  /**
   * @return the local tile id within its tileset.
   */
  public int id() {
    return id;
  }

  /**
   * @return the tile's class/type, or {@code null} when unset.
   */
  public String type() {
    return type;
  }

  /**
   * @return the tile's selection probability, defaulting to {@code 1}.
   */
  public double probability() {
    return probability != null ? probability : 1.0;
  }

  /**
   * @return this tile's own image, present only for image-collection tilesets.
   */
  public Optional<TmxImage> image() {
    return Optional.ofNullable(image);
  }

  /**
   * @return the tile's custom properties, if any.
   */
  public Optional<Properties> properties() {
    return Optional.ofNullable(properties);
  }
}
