package com.cryptroot.tiled.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A tileset used by a map (a {@code <tileset>} element).
 *
 * <p>A tileset may be defined inline in the map, or referenced from an external TSX file via {@link
 * #source()}. In the external case the map only supplies {@link #firstGid()} and {@code source};
 * the remaining fields are folded in from the loaded TSX by {@link #mergeExternal(TmxTileset)}
 * during parsing, so that a fully-parsed {@link TmxMap} always exposes self-contained tilesets. The
 * {@code source} is retained after resolution so that image paths (which are relative to the TSX
 * file) can be resolved against the correct directory.
 *
 * <p>Two tileset kinds are supported:
 *
 * <ul>
 *   <li>a single-image tileset with one {@link #image()} sliced into a grid, and
 *   <li>an image-collection tileset where each {@link TmxTile} carries its own image.
 * </ul>
 *
 * <p>This is a parsed, read-only data holder, except for the {@link #mergeExternal} hook invoked by
 * the parser.
 */
public final class TmxTileset {

  @JacksonXmlProperty(isAttribute = true)
  private int firstgid;

  @JacksonXmlProperty(isAttribute = true)
  private String source;

  @JacksonXmlProperty(isAttribute = true)
  private String name;

  @JacksonXmlProperty(isAttribute = true)
  private int tilewidth;

  @JacksonXmlProperty(isAttribute = true)
  private int tileheight;

  @JacksonXmlProperty(isAttribute = true)
  private int spacing;

  @JacksonXmlProperty(isAttribute = true)
  private int margin;

  @JacksonXmlProperty(isAttribute = true)
  private int tilecount;

  @JacksonXmlProperty(isAttribute = true)
  private int columns;

  @JacksonXmlProperty(localName = "image")
  private TmxImage image;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "tile")
  private List<TmxTile> tile = new ArrayList<>();

  @JacksonXmlProperty(localName = "properties")
  private Properties properties;

  /**
   * @return the first global tile id this tileset maps to (1-based across the map).
   */
  public int firstGid() {
    return firstgid;
  }

  /**
   * @return the external TSX path (relative to the map file) this tileset was defined in, or {@code
   *     null} for an inline tileset. It is retained after resolution so image paths can be resolved
   *     relative to the TSX file's directory.
   */
  public String source() {
    return source;
  }

  /**
   * @return the tileset name, or {@code null} when unset.
   */
  public String name() {
    return name;
  }

  /**
   * @return the tile width in pixels.
   */
  public int tileWidth() {
    return tilewidth;
  }

  /**
   * @return the tile height in pixels.
   */
  public int tileHeight() {
    return tileheight;
  }

  /**
   * @return the spacing in pixels between tiles in the image, defaulting to {@code 0}.
   */
  public int spacing() {
    return spacing;
  }

  /**
   * @return the margin in pixels around the tiles in the image, defaulting to {@code 0}.
   */
  public int margin() {
    return margin;
  }

  /**
   * @return the number of tiles in the tileset.
   */
  public int tileCount() {
    return tilecount;
  }

  /**
   * @return the number of tile columns in the image.
   */
  public int columns() {
    return columns;
  }

  /**
   * @return the single tileset image, present for single-image tilesets.
   */
  public Optional<TmxImage> image() {
    return Optional.ofNullable(image);
  }

  /**
   * @return an unmodifiable view of per-tile definitions, in document order.
   */
  public List<TmxTile> tiles() {
    return Collections.unmodifiableList(tile);
  }

  /**
   * @return the tileset's custom properties, if any.
   */
  public Optional<Properties> properties() {
    return Optional.ofNullable(properties);
  }

  /**
   * @return {@code true} when this is an image-collection tileset (no single image, each tile
   *     supplies its own).
   */
  public boolean isCollection() {
    return image == null;
  }

  /**
   * @return the local tile id for a raw global tile id (flip flags must already be stripped),
   *     computed as {@code gid - firstGid()}.
   */
  public int localId(int gidWithoutFlags) {
    return gidWithoutFlags - firstgid;
  }

  /**
   * Folds the definition loaded from an external TSX file into this reference, preserving this
   * reference's {@link #firstGid()} and {@link #source()} (the latter so image paths can still be
   * resolved relative to the TSX file's directory).
   *
   * <p>Invoked by the parser; not intended for general use.
   *
   * @param def the tileset parsed from the referenced TSX file
   */
  public void mergeExternal(TmxTileset def) {
    Objects.requireNonNull(def, "def must not be null");
    this.name = def.name;
    this.tilewidth = def.tilewidth;
    this.tileheight = def.tileheight;
    this.spacing = def.spacing;
    this.margin = def.margin;
    this.tilecount = def.tilecount;
    this.columns = def.columns;
    this.image = def.image;
    this.tile = def.tile;
    this.properties = def.properties;
  }
}
