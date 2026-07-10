package com.cryptroot.tiled.io;

import com.cryptroot.tiled.model.LayerData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Decodes the raw payload of a {@link LayerData} into an array of raw global tile ids (gids), one
 * per cell in row-major, left-to-right, top-to-bottom order.
 *
 * <p>Supported encodings:
 *
 * <ul>
 *   <li>{@code csv} — comma-separated decimal gids.
 *   <li>{@code base64} — base64 text decoding to little-endian unsigned 32-bit gids, optionally
 *       compressed with {@code gzip} or {@code zlib}.
 * </ul>
 *
 * <p>The deprecated per-{@code <tile>} XML form and {@code zstd} compression are not supported and
 * raise {@link UnsupportedOperationException}. The returned gids still contain their flip flags;
 * use {@link GlobalTileId} to interpret them.
 */
public final class TileDataCodec {

  private TileDataCodec() {}

  /**
   * Decodes {@code data} into {@code expectedCount} gids.
   *
   * @param data the raw layer payload
   * @param expectedCount the number of cells expected ({@code width * height})
   * @return an array of {@code expectedCount} raw gids
   * @throws IllegalArgumentException if the decoded cell count does not match
   * @throws UnsupportedOperationException for unsupported encodings/compression
   */
  public static int[] decode(LayerData data, int expectedCount) {
    if (data == null) {
      throw new IllegalArgumentException("layer has no <data> element");
    }
    String encoding = data.encoding();
    String text = data.text() == null ? "" : data.text();
    if (encoding == null) {
      throw new UnsupportedOperationException(
          "Unencoded tile data (per-<tile> XML) is not supported; use csv or base64");
    }
    return switch (encoding.trim().toLowerCase()) {
      case "csv" -> decodeCsv(text, expectedCount);
      case "base64" -> decodeBase64(text, data.compression(), expectedCount);
      default ->
          throw new UnsupportedOperationException(
              "Unsupported tile data encoding: "
                  + encoding
                  + " (only csv and base64 are supported)");
    };
  }

  private static int[] decodeCsv(String text, int expectedCount) {
    int[] out = new int[expectedCount];
    int count = 0;
    int len = text.length();
    int i = 0;
    while (i < len) {
      char c = text.charAt(i);
      if (c == ',' || Character.isWhitespace(c)) {
        i++;
        continue;
      }
      int start = i;
      while (i < len) {
        char d = text.charAt(i);
        if (d == ',' || Character.isWhitespace(d)) {
          break;
        }
        i++;
      }
      if (count >= expectedCount) {
        throw new IllegalArgumentException(
            "CSV tile data has more entries than the expected " + expectedCount);
      }
      out[count++] = (int) Long.parseLong(text.substring(start, i));
    }
    if (count != expectedCount) {
      throw new IllegalArgumentException(
          "CSV tile data has " + count + " entries, expected " + expectedCount);
    }
    return out;
  }

  private static int[] decodeBase64(String text, String compression, int expectedCount) {
    byte[] raw = Base64.getDecoder().decode(stripWhitespace(text));
    byte[] bytes = decompress(raw, compression);
    if (bytes.length != expectedCount * 4) {
      throw new IllegalArgumentException(
          "base64 tile data has " + (bytes.length / 4) + " entries, expected " + expectedCount);
    }
    int[] out = new int[expectedCount];
    for (int i = 0; i < expectedCount; i++) {
      int o = i * 4;
      out[i] =
          (bytes[o] & 0xFF)
              | ((bytes[o + 1] & 0xFF) << 8)
              | ((bytes[o + 2] & 0xFF) << 16)
              | ((bytes[o + 3] & 0xFF) << 24);
    }
    return out;
  }

  private static byte[] decompress(byte[] raw, String compression) {
    if (compression == null || compression.isBlank()) {
      return raw;
    }
    return switch (compression.trim().toLowerCase()) {
      case "gzip" -> readFully(wrap(raw, "gzip"));
      case "zlib" -> readFully(wrap(raw, "zlib"));
      case "zstd" ->
          throw new UnsupportedOperationException(
              "zstd tile data compression is not supported; use gzip, zlib or no compression");
      default ->
          throw new UnsupportedOperationException(
              "Unsupported tile data compression: " + compression);
    };
  }

  private static InputStream wrap(byte[] raw, String compression) {
    try {
      ByteArrayInputStream in = new ByteArrayInputStream(raw);
      return "gzip".equals(compression) ? new GZIPInputStream(in) : new InflaterInputStream(in);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to open " + compression + " stream", e);
    }
  }

  private static byte[] readFully(InputStream in) {
    try (in) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to decompress tile data", e);
    }
  }

  private static String stripWhitespace(String text) {
    StringBuilder sb = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
