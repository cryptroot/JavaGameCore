package com.cryptroot.tiled.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.tiled.model.ObjectGroup;
import com.cryptroot.tiled.model.Properties;
import com.cryptroot.tiled.model.TileLayer;
import com.cryptroot.tiled.model.TmxLayer;
import com.cryptroot.tiled.model.TmxMap;
import com.cryptroot.tiled.model.TmxObject;
import com.cryptroot.tiled.model.TmxTileset;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Parses a fixture exercising Standard-scope features: an embedded tileset, map/object custom
 * properties, an object group, and — importantly — the preservation of layer document order across
 * a tile layer and an interleaved object group.
 */
class EmbeddedMapTest {

  private TmxMap parseEmbedded() throws IOException {
    return new TmxParser().parse("assets/test/Embedded.tmx");
  }

  @Test
  void readsMapProperties() throws IOException {
    Properties props = parseEmbedded().properties().orElseThrow();
    assertEquals("cave", props.getString("ambient", null));
    assertEquals(3, props.getInt("difficulty", 0));
    assertTrue(props.getBool("spooky", false));
  }

  @Test
  void readsEmbeddedTileset() throws IOException {
    List<TmxTileset> tilesets = parseEmbedded().tilesets();
    assertEquals(1, tilesets.size());

    TmxTileset tileset = tilesets.get(0);
    assertEquals(1, tileset.firstGid());
    assertEquals("Inline", tileset.name());
    assertFalse(tileset.isCollection());
    assertEquals("Cave_Tilemap.png", tileset.image().orElseThrow().source());
  }

  @Test
  void preservesLayerDocumentOrder() throws IOException {
    List<TmxLayer> layers = parseEmbedded().layers();
    assertEquals(2, layers.size());
    assertInstanceOf(TileLayer.class, layers.get(0));
    assertInstanceOf(ObjectGroup.class, layers.get(1));
    assertEquals("Ground", layers.get(0).name());
    assertEquals("Objects", layers.get(1).name());
  }

  @Test
  void decodesEmbeddedLayer() throws IOException {
    TileLayer ground = parseEmbedded().tileLayers().get(0);
    int[] gids = TileDataCodec.decode(ground.data(), ground.width() * ground.height());
    assertEquals(4, gids.length);
    assertEquals(1, gids[0]);
    assertEquals(2, gids[1]);
    assertEquals(3, gids[2]);
    assertEquals(4, gids[3]);
  }

  @Test
  void readsObjectsAndShapes() throws IOException {
    ObjectGroup group = parseEmbedded().objectGroups().get(0);
    List<TmxObject> objects = group.objects();
    assertEquals(3, objects.size());

    TmxObject spawn = objects.get(0);
    assertEquals("spawn", spawn.name());
    assertEquals("PlayerStart", spawn.type());
    assertEquals(TmxObject.Shape.POINT, spawn.shape());
    assertEquals(16.0, spawn.x());
    assertEquals(48.0, spawn.y());

    TmxObject trigger = objects.get(1);
    assertEquals(TmxObject.Shape.RECTANGLE, trigger.shape());
    assertEquals(64.0, trigger.width());
    assertEquals("cave_intro", trigger.properties().orElseThrow().getString("event", null));

    TmxObject torch = objects.get(2);
    assertEquals(TmxObject.Shape.TILE, torch.shape());
    assertTrue(torch.gid().isPresent());
    assertEquals(2, torch.gid().getAsLong());
  }
}
