package com.cryptroot.tiled.io;

import com.cryptroot.tiled.model.Orientation;
import com.cryptroot.tiled.model.TmxMap;
import com.cryptroot.tiled.model.TmxTileset;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads Tiled TMX maps (and their referenced TSX tilesets) from the classpath into the {@code
 * model} object graph.
 *
 * <p>Parsing is GL-free and has no libGDX dependency: resources are read through the class loader,
 * external tilesets are resolved and folded into their map references, but tile data is left
 * undecoded and no textures are loaded. Rendering concerns live in the {@code render} layer.
 *
 * <p>A single {@code TmxParser} may be reused to parse multiple maps; it is not thread-safe.
 */
public final class TmxParser {

  private final XmlMapper mapper;

  /** Creates a parser with an {@link XmlMapper} configured for the TMX/TSX schema. */
  public TmxParser() {
    this.mapper = new XmlMapper();
    this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(TmxMap.class, new TmxMapDeserializer());
    this.mapper.registerModule(module);
  }

  /**
   * Parses the TMX map at the given classpath location, resolving any external tilesets relative to
   * the map's location.
   *
   * @param classpathResource the classpath of the {@code .tmx} file, e.g. {@code
   *     "assets/test/Cave.tmx"}
   * @return the fully-parsed, tileset-resolved map
   * @throws IOException if the map or a referenced tileset cannot be read or parsed
   */
  public TmxMap parse(String classpathResource) throws IOException {
    byte[] bytes = readClasspath(classpathResource);
    TmxMap map = mapper.readValue(bytes, TmxMap.class);
    resolveExternalTilesets(map, classpathResource);
    return map;
  }

  /**
   * Parses a standalone TSX tileset at the given classpath location.
   *
   * @param classpathResource the classpath of the {@code .tsx} file
   * @return the parsed tileset (its {@code firstgid} is {@code 0}, since that is a map-specific
   *     attribute not present in a standalone TSX)
   * @throws IOException if the tileset cannot be read or parsed
   */
  public TmxTileset parseTileset(String classpathResource) throws IOException {
    return mapper.readValue(readClasspath(classpathResource), TmxTileset.class);
  }

  private void resolveExternalTilesets(TmxMap map, String mapPath) throws IOException {
    for (TmxTileset tileset : map.tilesets()) {
      if (tileset.source() != null) {
        String tsxPath = ResourceLocator.resolve(mapPath, tileset.source());
        TmxTileset definition = mapper.readValue(readClasspath(tsxPath), TmxTileset.class);
        tileset.mergeExternal(definition);
      }
    }
  }

  private static byte[] readClasspath(String resource) throws IOException {
    ClassLoader loader = TmxParser.class.getClassLoader();
    try (InputStream in = loader.getResourceAsStream(resource)) {
      if (in == null) {
        throw new FileNotFoundException("Classpath resource not found: " + resource);
      }
      return in.readAllBytes();
    }
  }

  /**
   * @return {@code true} if the map's orientation is one this library can render. Non-orthogonal
   *     maps parse successfully but cannot be turned into render components.
   */
  public static boolean isRenderable(TmxMap map) {
    return map.orientation() == Orientation.ORTHOGONAL && !map.infinite();
  }
}
