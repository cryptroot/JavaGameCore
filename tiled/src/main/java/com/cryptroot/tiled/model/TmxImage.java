package com.cryptroot.tiled.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * A reference to an image file (an {@code <image>} element), used either by a single-image {@link
 * TmxTileset} or by an individual {@link TmxTile} in an image collection tileset.
 *
 * <p>Only external images (referenced by {@code source}) are supported; embedded image data is out
 * of scope.
 *
 * <p>This is a parsed, read-only data holder.
 */
public final class TmxImage {

  @JacksonXmlProperty(isAttribute = true)
  private String source;

  @JacksonXmlProperty(isAttribute = true)
  private int width;

  @JacksonXmlProperty(isAttribute = true)
  private int height;

  @JacksonXmlProperty(isAttribute = true)
  private String trans;

  @JacksonXmlProperty(isAttribute = true)
  private String format;

  /**
   * @return the image file path, relative to the file that referenced it.
   */
  public String source() {
    return source;
  }

  /**
   * @return the image width in pixels, or {@code 0} when not declared.
   */
  public int width() {
    return width;
  }

  /**
   * @return the image height in pixels, or {@code 0} when not declared.
   */
  public int height() {
    return height;
  }

  /**
   * @return the colour treated as transparent (e.g. {@code "FF00FF"}), or {@code null} when no
   *     transparent colour is declared.
   */
  public String trans() {
    return trans;
  }

  /**
   * @return the embedded-image format hint, or {@code null} for external images.
   */
  public String format() {
    return format;
  }
}
