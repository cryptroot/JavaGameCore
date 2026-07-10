package com.cryptroot.tiled.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.tiled.model.Orientation;
import com.cryptroot.tiled.model.RenderOrder;
import com.cryptroot.tiled.model.TileLayer;
import com.cryptroot.tiled.model.TmxImage;
import com.cryptroot.tiled.model.TmxMap;
import com.cryptroot.tiled.model.TmxTileset;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** End-to-end parse of the bundled {@code Cave.tmx} fixture and its external TSX. */
class TmxParserTest {

  private static final String CAVE = "assets/test/Cave.tmx";

  private TmxMap parseCave() throws IOException {
    return new TmxParser().parse(CAVE);
  }

  @Test
  void parsesMapHeader() throws IOException {
    TmxMap map = parseCave();
    assertEquals(Orientation.ORTHOGONAL, map.orientation());
    assertEquals(RenderOrder.RIGHT_DOWN, map.renderOrder());
    assertEquals(20, map.width());
    assertEquals(24, map.height());
    assertEquals(64, map.tileWidth());
    assertEquals(64, map.tileHeight());
    assertFalse(map.infinite());
    assertTrue(TmxParser.isRenderable(map));
  }

  @Test
  void resolvesExternalTileset() throws IOException {
    TmxMap map = parseCave();
    assertEquals(1, map.tilesets().size());

    TmxTileset tileset = map.tilesets().get(0);
    assertEquals(1, tileset.firstGid());
    assertEquals(
        "Cave_Tilemap.tsx",
        tileset.source(),
        "source is retained so tileset images resolve relative to the TSX directory");
    assertEquals("Cave_Tilemap", tileset.name());
    assertEquals(64, tileset.tileWidth());
    assertEquals(64, tileset.tileHeight());
    assertEquals(4, tileset.tileCount());
    assertEquals(2, tileset.columns());
    assertFalse(tileset.isCollection());

    TmxImage image = tileset.image().orElseThrow();
    assertEquals("Cave_Tilemap.png", image.source());
    assertEquals(128, image.width());
    assertEquals(128, image.height());
  }

  @Test
  void parsesSingleTileLayer() throws IOException {
    TmxMap map = parseCave();
    assertEquals(1, map.layers().size());
    assertEquals(1, map.tileLayers().size());
    assertTrue(map.objectGroups().isEmpty());

    TileLayer layer = map.tileLayers().get(0);
    assertEquals("Tile Layer 1", layer.name());
    assertEquals(20, layer.width());
    assertEquals(24, layer.height());
    assertEquals("csv", layer.data().encoding());
  }

  @Test
  void decodesTileLayerData() throws IOException {
    TmxMap map = parseCave();
    TileLayer layer = map.tileLayers().get(0);

    int[] gids = TileDataCodec.decode(layer.data(), layer.width() * layer.height());
    assertEquals(480, gids.length);

    // First row of Cave.tmx: 2,2,3,3,1,...,1,3,3,2,2
    assertEquals(2, gids[0]);
    assertEquals(2, gids[1]);
    assertEquals(3, gids[2]);
    assertEquals(3, gids[3]);
    assertEquals(1, gids[4]);
    assertEquals(3, gids[16]);
    assertEquals(3, gids[17]);
    assertEquals(2, gids[18]);
    assertEquals(2, gids[19]);
  }

  @Test
  void resolvesTilesetForGid() throws IOException {
    TmxMap map = parseCave();
    TmxTileset tileset = map.tilesets().get(0);

    assertSame(tileset, map.tilesetForGid(1).orElseThrow());
    assertSame(tileset, map.tilesetForGid(4).orElseThrow());
    assertEquals(0, tileset.localId(1));
    assertEquals(2, tileset.localId(3));
    assertTrue(map.tilesetForGid(0).isEmpty(), "gid 0 is the empty tile");
  }
}
