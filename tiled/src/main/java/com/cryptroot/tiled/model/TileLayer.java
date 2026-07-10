package com.cryptroot.tiled.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * A grid of tiles (a {@code <layer>} element).
 *
 * <p>The tile payload is exposed as raw {@link #data()}; decoding it into global tile ids is
 * performed by the {@code io} layer, keyed by {@link #width()} and {@link #height()}.
 *
 * <p>This is a parsed, read-only data holder.
 */
public final class TileLayer extends TmxLayer {

  @JacksonXmlProperty(isAttribute = true)
  private int width;

  @JacksonXmlProperty(isAttribute = true)
  private int height;

  @JacksonXmlProperty(localName = "data")
  private LayerData data;

  /**
   * @return the layer width in tiles (equal to the map width for fixed-size maps).
   */
  public int width() {
    return width;
  }

  /**
   * @return the layer height in tiles (equal to the map height for fixed-size maps).
   */
  public int height() {
    return height;
  }

  /**
   * @return the raw, undecoded tile payload, or {@code null} when the layer is empty.
   */
  public LayerData data() {
    return data;
  }
}
