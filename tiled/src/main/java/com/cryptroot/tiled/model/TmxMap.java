package com.cryptroot.tiled.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A fully-parsed Tiled map (a {@code <map>} element).
 *
 * <p>Instances are immutable aggregates assembled by the parser after external tilesets have been
 * resolved, so every {@link TmxTileset} returned by {@link #tilesets()} is self-contained. Layers
 * are returned in document order, which is also their draw order.
 *
 * <p>Only orthogonal maps are rendered; the {@link #orientation()} is still reported for other
 * kinds so callers can decide how to handle them.
 */
public final class TmxMap {

  private final String version;
  private final String tiledVersion;
  private final Orientation orientation;
  private final RenderOrder renderOrder;
  private final int width;
  private final int height;
  private final int tileWidth;
  private final int tileHeight;
  private final boolean infinite;
  private final String backgroundColor;
  private final List<TmxTileset> tilesets;
  private final List<TmxLayer> layers;
  private final Properties properties;

  /**
   * Creates a fully-resolved map. Intended for use by the parser; the supplied collections are
   * defensively copied.
   */
  public TmxMap(
      String version,
      String tiledVersion,
      Orientation orientation,
      RenderOrder renderOrder,
      int width,
      int height,
      int tileWidth,
      int tileHeight,
      boolean infinite,
      String backgroundColor,
      List<TmxTileset> tilesets,
      List<TmxLayer> layers,
      Properties properties) {
    this.version = version;
    this.tiledVersion = tiledVersion;
    this.orientation = orientation;
    this.renderOrder = renderOrder;
    this.width = width;
    this.height = height;
    this.tileWidth = tileWidth;
    this.tileHeight = tileHeight;
    this.infinite = infinite;
    this.backgroundColor = backgroundColor;
    this.tilesets = List.copyOf(tilesets);
    this.layers = List.copyOf(layers);
    this.properties = properties;
  }

  /**
   * @return the TMX format version, or {@code null} when unset.
   */
  public String version() {
    return version;
  }

  /**
   * @return the Tiled version that saved the file, or {@code null} when unset.
   */
  public String tiledVersion() {
    return tiledVersion;
  }

  /**
   * @return the map orientation.
   */
  public Orientation orientation() {
    return orientation;
  }

  /**
   * @return the tile render order.
   */
  public RenderOrder renderOrder() {
    return renderOrder;
  }

  /**
   * @return the map width in tiles.
   */
  public int width() {
    return width;
  }

  /**
   * @return the map height in tiles.
   */
  public int height() {
    return height;
  }

  /**
   * @return the width of a tile in pixels.
   */
  public int tileWidth() {
    return tileWidth;
  }

  /**
   * @return the height of a tile in pixels.
   */
  public int tileHeight() {
    return tileHeight;
  }

  /**
   * @return whether the map is infinite (stored in chunks); always unsupported for rendering.
   */
  public boolean infinite() {
    return infinite;
  }

  /**
   * @return the background colour in {@code #AARRGGBB} or {@code #RRGGBB} form, or {@code null}
   *     when the map has no background colour.
   */
  public String backgroundColor() {
    return backgroundColor;
  }

  /**
   * @return an unmodifiable list of resolved tilesets, ascending by first global id.
   */
  public List<TmxTileset> tilesets() {
    return tilesets;
  }

  /**
   * @return an unmodifiable list of all layers, in document (draw) order.
   */
  public List<TmxLayer> layers() {
    return layers;
  }

  /**
   * @return the map's custom properties, if any.
   */
  public Optional<Properties> properties() {
    return Optional.ofNullable(properties);
  }

  /**
   * @return only the {@link TileLayer} layers, in document order.
   */
  public List<TileLayer> tileLayers() {
    List<TileLayer> out = new ArrayList<>();
    for (TmxLayer layer : layers) {
      if (layer instanceof TileLayer tl) {
        out.add(tl);
      }
    }
    return out;
  }

  /**
   * @return only the {@link ObjectGroup} layers, in document order.
   */
  public List<ObjectGroup> objectGroups() {
    List<ObjectGroup> out = new ArrayList<>();
    for (TmxLayer layer : layers) {
      if (layer instanceof ObjectGroup og) {
        out.add(og);
      }
    }
    return out;
  }

  /**
   * Selects the tileset that owns the given raw global tile id, i.e. the tileset with the greatest
   * {@link TmxTileset#firstGid()} that is less than or equal to the id.
   *
   * @param gidWithoutFlags a global tile id with its flip flags already stripped (see the {@code
   *     io} layer's global-tile-id helper)
   * @return the owning tileset, or empty for id {@code 0} (the empty tile) or when no tileset
   *     matches
   */
  public Optional<TmxTileset> tilesetForGid(int gidWithoutFlags) {
    if (gidWithoutFlags <= 0) {
      return Optional.empty();
    }
    TmxTileset best = null;
    for (TmxTileset ts : tilesets) {
      if (ts.firstGid() <= gidWithoutFlags && (best == null || ts.firstGid() > best.firstGid())) {
        best = ts;
      }
    }
    return Optional.ofNullable(best);
  }
}
