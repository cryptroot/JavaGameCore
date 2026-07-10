package com.cryptroot.tiled.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.grid.Grid;
import com.cryptroot.tiled.io.TileGeometry;
import com.cryptroot.tiled.io.TmxParser;
import com.cryptroot.tiled.model.TmxMap;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class TiledGridsTest {

  private TmxMap map() throws IOException {
    return new TmxParser().parse("assets/test/Embedded.tmx");
  }

  @Test
  void gridDimensionsMatchMap() throws IOException {
    TmxMap map = map();
    Grid grid = TiledGrids.fromMap(map);
    assertEquals(map.width(), grid.columns());
    assertEquals(map.height(), grid.rows());
    assertEquals(map.tileWidth(), grid.cellWidth(), 1e-4f);
    assertEquals(map.tileHeight(), grid.cellHeight(), 1e-4f);
  }

  @Test
  void gridCellCentersAlignWithTileGeometry() throws IOException {
    TmxMap map = map();
    int mapH = map.height(), tw = map.tileWidth(), th = map.tileHeight();
    Grid grid = TiledGrids.fromMap(map);

    for (int col = 0; col < map.width(); col++) {
      for (int row = 0; row < map.height(); row++) {
        Vector2 c = grid.cellToWorld(col, row);
        // Grid row r (y-up from bottom) == Tiled row (mapH-1-r).
        float expectedX = TileGeometry.worldX(col, tw) + tw / 2f;
        float expectedY = TileGeometry.worldY(mapH - 1 - row, mapH, th) + th / 2f;
        assertEquals(expectedX, c.x, 1e-3f);
        assertEquals(expectedY, c.y, 1e-3f);
      }
    }
  }
}
