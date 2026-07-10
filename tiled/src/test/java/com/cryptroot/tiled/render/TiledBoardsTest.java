package com.cryptroot.tiled.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.path.Board;
import com.cryptroot.tiled.io.TmxParser;
import com.cryptroot.tiled.model.TileLayer;
import com.cryptroot.tiled.model.TmxMap;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class TiledBoardsTest {

  private TmxMap map() throws IOException {
    return new TmxParser().parse("assets/test/Embedded.tmx");
  }

  @Test
  void flipsTiledTopDownRowsIntoGridBottomUpRows() throws IOException {
    TmxMap map = map();
    TileLayer ground = map.tileLayers().get(0);
    // Embedded.tmx "Ground" layer, Tiled row-major top-down: row0="1,2", row1="3,4".
    Board board = TiledBoards.fromLayer(map, ground, gid -> gid == 4);

    // core.grid row 0 = world bottom = Tiled's LAST row ("3,4") -> only gid 4 at (col=1,row=0).
    assertTrue(board.isBlocked(1, 0), "grid (1,0) is Tiled's bottom-right cell, gid 4");
    assertFalse(board.isBlocked(0, 0), "grid (0,0) is Tiled's bottom-left cell, gid 3");
    assertFalse(board.isBlocked(0, 1), "grid (0,1) is Tiled's top-left cell, gid 1");
    assertFalse(board.isBlocked(1, 1), "grid (1,1) is Tiled's top-right cell, gid 2");
  }

  @Test
  void stripsFlipFlagsBeforeTestingPredicate() throws IOException {
    TmxMap map = map();
    TileLayer ground = map.tileLayers().get(0);
    // No cell in the fixture is actually flipped, but the predicate must see the clean id (1..4)
    // for every cell, never a raw gid with flip-flag bits set.
    Board board = TiledBoards.fromLayer(map, ground, gid -> gid < 1 || gid > 4);
    for (int col = 0; col < map.width(); col++) {
      for (int row = 0; row < map.height(); row++) {
        assertFalse(board.isBlocked(col, row), "gid at (" + col + "," + row + ") should be in [1,4]");
      }
    }
  }
}
