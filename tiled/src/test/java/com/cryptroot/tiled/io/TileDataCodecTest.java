package com.cryptroot.tiled.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cryptroot.tiled.model.LayerData;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

/** Decoding tests for {@link TileDataCodec} across CSV and base64 (raw/gzip/zlib). */
class TileDataCodecTest {

  private static final XmlMapper XML = new XmlMapper();

  @Test
  void decodesCsv() {
    int[] gids = TileDataCodec.decode(layerData("csv", null, "1,2,3,4"), 4);
    assertArrayEquals(new int[] {1, 2, 3, 4}, gids);
  }

  @Test
  void decodesCsvIgnoringSurroundingWhitespace() {
    int[] gids = TileDataCodec.decode(layerData("csv", null, "\n 1,2,3,\n 4 \n"), 4);
    assertArrayEquals(new int[] {1, 2, 3, 4}, gids);
  }

  @Test
  void decodesUncompressedBase64() {
    int[] expected = {1, 2, 3, 4};
    int[] gids = TileDataCodec.decode(layerData("base64", null, base64(expected, null)), 4);
    assertArrayEquals(expected, gids);
  }

  @Test
  void decodesGzipBase64() {
    int[] expected = {1, 2, 3, 4};
    int[] gids = TileDataCodec.decode(layerData("base64", "gzip", base64(expected, "gzip")), 4);
    assertArrayEquals(expected, gids);
  }

  @Test
  void decodesZlibBase64() {
    int[] expected = {1, 2, 3, 4};
    int[] gids = TileDataCodec.decode(layerData("base64", "zlib", base64(expected, "zlib")), 4);
    assertArrayEquals(expected, gids);
  }

  @Test
  void preservesFlipFlagsInHighBitsAsLittleEndian() {
    int flipped = 3 | GlobalTileId.FLIP_HORIZONTAL | GlobalTileId.FLIP_DIAGONAL;
    int[] expected = {flipped};
    int[] gids = TileDataCodec.decode(layerData("base64", "zlib", base64(expected, "zlib")), 1);
    assertArrayEquals(expected, gids);
  }

  @Test
  void rejectsWrongCellCount() {
    LayerData data = layerData("csv", null, "1,2,3");
    assertThrows(IllegalArgumentException.class, () -> TileDataCodec.decode(data, 4));
  }

  // -- helpers ------------------------------------------------------------

  private static LayerData layerData(String encoding, String compression, String text) {
    String comp = compression == null ? "" : " compression=\"" + compression + "\"";
    String xml = "<data encoding=\"" + encoding + "\"" + comp + ">" + text + "</data>";
    try {
      return XML.readValue(xml, LayerData.class);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String base64(int[] gids, String compression) {
    ByteArrayOutputStream raw = new ByteArrayOutputStream();
    for (int g : gids) {
      raw.write(g & 0xFF);
      raw.write((g >> 8) & 0xFF);
      raw.write((g >> 16) & 0xFF);
      raw.write((g >> 24) & 0xFF);
    }
    byte[] bytes = raw.toByteArray();
    byte[] payload;
    try {
      if ("gzip".equals(compression)) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
          gz.write(bytes);
        }
        payload = out.toByteArray();
      } else if ("zlib".equals(compression)) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DeflaterOutputStream df = new DeflaterOutputStream(out)) {
          df.write(bytes);
        }
        payload = out.toByteArray();
      } else {
        payload = bytes;
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return Base64.getEncoder().encodeToString(payload);
  }
}
