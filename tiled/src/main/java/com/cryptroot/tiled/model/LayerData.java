package com.cryptroot.tiled.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

/**
 * The raw tile payload of a {@link TileLayer} (a {@code <data>} element).
 *
 * <p>Carries the declared {@link #encoding()} and {@link #compression()} together with the
 * undecoded {@link #text()} content. Turning this into an array of global tile ids is the
 * responsibility of the {@code io} decoding layer, which keeps this model free of any decoding
 * logic.
 *
 * <p>Supported encodings are {@code csv} and {@code base64}; the deprecated per-{@code <tile>} XML
 * form is not supported (its {@link #text()} is empty).
 *
 * <p>This is a parsed, read-only data holder.
 */
public final class LayerData {

  @JacksonXmlProperty(isAttribute = true)
  private String encoding;

  @JacksonXmlProperty(isAttribute = true)
  private String compression;

  @JacksonXmlText private String text;

  /**
   * @return the encoding (e.g. {@code "csv"} or {@code "base64"}), or {@code null}.
   */
  public String encoding() {
    return encoding;
  }

  /**
   * @return the compression (e.g. {@code "gzip"} or {@code "zlib"}), or {@code null}.
   */
  public String compression() {
    return compression;
  }

  /**
   * @return the raw, undecoded payload text, or {@code null} when absent.
   */
  public String text() {
    return text;
  }
}
