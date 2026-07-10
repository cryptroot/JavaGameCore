package com.cryptroot.tiled.io;

import com.cryptroot.tiled.model.ObjectGroup;
import com.cryptroot.tiled.model.Orientation;
import com.cryptroot.tiled.model.Properties;
import com.cryptroot.tiled.model.RenderOrder;
import com.cryptroot.tiled.model.TileLayer;
import com.cryptroot.tiled.model.TmxLayer;
import com.cryptroot.tiled.model.TmxMap;
import com.cryptroot.tiled.model.TmxTileset;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Jackson deserializer for {@link TmxMap} that walks the children of the {@code <map>}
 * element in document order.
 *
 * <p>Tile layers ({@code <layer>}) and object groups ({@code <objectgroup>}) can be interleaved,
 * and their relative order defines the draw order. Standard POJO mapping would split them into
 * separate lists and lose that ordering, so this deserializer streams the child elements and
 * appends layers to a single ordered list as they are encountered, delegating each child element to
 * the mapper's default (annotation-based) deserialization.
 *
 * <p>Unsupported layer kinds ({@code <imagelayer>}, {@code <group>}) and editor metadata are
 * skipped rather than causing a failure, per the TMX recommendation to ignore unknown content.
 */
public final class TmxMapDeserializer extends JsonDeserializer<TmxMap> {

  @Override
  public TmxMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String version = null;
    String tiledVersion = null;
    String orientation = null;
    String renderOrder = null;
    String backgroundColor = null;
    int width = 0;
    int height = 0;
    int tileWidth = 0;
    int tileHeight = 0;
    boolean infinite = false;

    List<TmxTileset> tilesets = new ArrayList<>();
    List<TmxLayer> layers = new ArrayList<>();
    Properties properties = null;

    for (JsonToken t = p.nextToken(); t != null && t != JsonToken.END_OBJECT; t = p.nextToken()) {
      if (t != JsonToken.FIELD_NAME) {
        continue;
      }
      String field = p.currentName();
      p.nextToken(); // advance to the field's value
      switch (field) {
        case "version" -> version = p.getValueAsString();
        case "tiledversion" -> tiledVersion = p.getValueAsString();
        case "orientation" -> orientation = p.getValueAsString();
        case "renderorder" -> renderOrder = p.getValueAsString();
        case "backgroundcolor" -> backgroundColor = p.getValueAsString();
        case "width" -> width = p.getValueAsInt();
        case "height" -> height = p.getValueAsInt();
        case "tilewidth" -> tileWidth = p.getValueAsInt();
        case "tileheight" -> tileHeight = p.getValueAsInt();
        case "infinite" -> infinite = p.getValueAsInt() != 0;
        case "tileset" -> tilesets.add(p.readValueAs(TmxTileset.class));
        case "layer" -> layers.add(p.readValueAs(TileLayer.class));
        case "objectgroup" -> layers.add(p.readValueAs(ObjectGroup.class));
        case "properties" -> properties = p.readValueAs(Properties.class);
        default -> skipValue(p);
      }
    }

    return new TmxMap(
        version,
        tiledVersion,
        Orientation.fromTmx(orientation),
        RenderOrder.fromTmx(renderOrder),
        width,
        height,
        tileWidth,
        tileHeight,
        infinite,
        backgroundColor,
        tilesets,
        layers,
        properties);
  }

  private static void skipValue(JsonParser p) throws IOException {
    JsonToken current = p.currentToken();
    if (current != null && current.isStructStart()) {
      p.skipChildren();
    }
  }
}
