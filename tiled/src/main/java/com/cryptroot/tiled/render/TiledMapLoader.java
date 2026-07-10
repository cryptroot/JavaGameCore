package com.cryptroot.tiled.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.resources.ResourceManager;
import com.cryptroot.tiled.io.ResourceLocator;
import com.cryptroot.tiled.io.TileDataCodec;
import com.cryptroot.tiled.io.TileRect;
import com.cryptroot.tiled.io.TileSliceMath;
import com.cryptroot.tiled.io.TmxParser;
import com.cryptroot.tiled.model.TileLayer;
import com.cryptroot.tiled.model.TmxImage;
import com.cryptroot.tiled.model.TmxMap;
import com.cryptroot.tiled.model.TmxTile;
import com.cryptroot.tiled.model.TmxTileset;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a TMX map from the classpath into a render-ready {@link TiledMap}: it parses the map, loads
 * and slices the tileset images into a {@link TileAtlas}, decodes each tile layer, and builds a
 * {@link TileLayerRenderComponent} per layer.
 *
 * <p>Textures are loaded and cached through the supplied {@link ResourceManager}, which retains
 * ownership of them. Sliced single-image tilesets default to {@link TextureFilter#Nearest}
 * filtering, which avoids the edge bleeding that linear filtering causes between adjacent tiles in
 * a shared image.
 *
 * <p>This loader requires an active libGDX GL context (it creates textures) and is therefore used
 * at runtime rather than in headless tests.
 */
public final class TiledMapLoader {

  private final ResourceManager resources;
  private final RenderPass renderPass;
  private final TextureFilter minFilter;
  private final TextureFilter magFilter;
  private final TmxParser parser = new TmxParser();

  /**
   * Creates a loader that places tile layers in {@link RenderPass#BACKGROUND} and uses {@link
   * TextureFilter#Nearest} filtering.
   *
   * @param resources the texture cache that will own the loaded tileset images
   */
  public TiledMapLoader(ResourceManager resources) {
    this(resources, RenderPass.BACKGROUND, TextureFilter.Nearest, TextureFilter.Nearest);
  }

  /**
   * @param resources the texture cache that will own the loaded tileset images
   * @param renderPass the pass to draw tile layers in
   * @param minFilter minification filter for tileset images
   * @param magFilter magnification filter for tileset images
   */
  public TiledMapLoader(
      ResourceManager resources,
      RenderPass renderPass,
      TextureFilter minFilter,
      TextureFilter magFilter) {
    this.resources = resources;
    this.renderPass = renderPass;
    this.minFilter = minFilter;
    this.magFilter = magFilter;
  }

  /**
   * Loads the TMX map at the given classpath location.
   *
   * @param tmxClasspath the classpath of the {@code .tmx} file, e.g. {@code "assets/test/Cave.tmx"}
   * @return a render-ready map
   * @throws IOException if the map or a referenced resource cannot be read
   * @throws UnsupportedOperationException if the map is not a finite orthogonal map
   */
  public TiledMap load(String tmxClasspath) throws IOException {
    TmxMap map = parser.parse(tmxClasspath);
    if (!TmxParser.isRenderable(map)) {
      throw new UnsupportedOperationException(
          "Only finite orthogonal maps can be rendered; got orientation="
              + map.orientation()
              + ", infinite="
              + map.infinite());
    }
    TileAtlas atlas = buildAtlas(map, tmxClasspath);
    List<TileLayerRenderComponent> layers = buildLayers(map, atlas);
    return new TiledMap(map, atlas, layers);
  }

  private TileAtlas buildAtlas(TmxMap map, String mapPath) {
    Map<Integer, TileAtlas.Tile> byGid = new HashMap<>();
    for (TmxTileset tileset : map.tilesets()) {
      if (tileset.isCollection()) {
        addCollectionTiles(byGid, tileset, mapPath);
      } else {
        addSingleImageTiles(byGid, tileset, mapPath);
      }
    }
    return new TileAtlas(byGid);
  }

  private void addSingleImageTiles(
      Map<Integer, TileAtlas.Tile> byGid, TmxTileset tileset, String mapPath) {
    TmxImage image = tileset.image().orElseThrow();
    Texture texture = loadTexture(imageClasspath(tileset, image.source(), mapPath));
    int tileWidth = tileset.tileWidth();
    int tileHeight = tileset.tileHeight();
    for (int localId = 0; localId < tileset.tileCount(); localId++) {
      TileRect rect =
          TileSliceMath.rect(
              localId,
              tileset.columns(),
              tileWidth,
              tileHeight,
              tileset.margin(),
              tileset.spacing());
      TextureRegion region =
          new TextureRegion(texture, rect.x(), rect.y(), rect.width(), rect.height());
      byGid.put(tileset.firstGid() + localId, new TileAtlas.Tile(region, tileWidth, tileHeight));
    }
  }

  private void addCollectionTiles(
      Map<Integer, TileAtlas.Tile> byGid, TmxTileset tileset, String mapPath) {
    for (TmxTile tile : tileset.tiles()) {
      if (tile.image().isEmpty()) {
        continue;
      }
      TmxImage image = tile.image().get();
      Texture texture = loadTexture(imageClasspath(tileset, image.source(), mapPath));
      TextureRegion region = new TextureRegion(texture);
      float width = image.width() > 0 ? image.width() : texture.getWidth();
      float height = image.height() > 0 ? image.height() : texture.getHeight();
      byGid.put(tileset.firstGid() + tile.id(), new TileAtlas.Tile(region, width, height));
    }
  }

  private List<TileLayerRenderComponent> buildLayers(TmxMap map, TileAtlas atlas) {
    List<TileLayerRenderComponent> out = new ArrayList<>();
    for (TileLayer layer : map.tileLayers()) {
      if (layer.data() == null) {
        continue;
      }
      int[] gids = TileDataCodec.decode(layer.data(), layer.width() * layer.height());
      out.add(
          new TileLayerRenderComponent(
              layer,
              gids,
              map.width(),
              map.height(),
              map.tileWidth(),
              map.tileHeight(),
              atlas,
              renderPass));
    }
    return out;
  }

  private Texture loadTexture(String classpath) {
    return resources.createTexture(classpath, minFilter, magFilter);
  }

  private static String imageClasspath(TmxTileset tileset, String imageSource, String mapPath) {
    String baseFile =
        tileset.source() != null ? ResourceLocator.resolve(mapPath, tileset.source()) : mapPath;
    return ResourceLocator.resolve(baseFile, imageSource);
  }
}
