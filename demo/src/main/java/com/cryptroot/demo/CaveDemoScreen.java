package com.cryptroot.demo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cryptroot.core.grid.Grid;
import com.cryptroot.core.path.Board;
import com.cryptroot.core.render.ShapeTextureFactory;
import com.cryptroot.core.screen.BaseGameScreen;
import com.cryptroot.demo.towerdefense.TowerDefenseController;
import com.cryptroot.tiled.model.TileLayer;
import com.cryptroot.tiled.render.TiledBoards;
import com.cryptroot.tiled.render.TiledGrids;
import com.cryptroot.tiled.render.TiledMap;
import com.cryptroot.tiled.render.TiledMapLoader;
import java.io.IOException;
import java.util.List;

/**
 * Demo screen that loads {@code Cave.tmx} and renders it through the core world pipeline, with a
 * simple tower-defense mini-game layered on top: click anywhere on the map to place a tower (a
 * translucent ghost previews the landing cell first), and towers shoot enemies that spawn on the
 * central light-brown floor lane at the bottom of the map and path toward the top. Space toggles
 * pause; 1/2 set normal/double speed (see {@link #timeScale}).
 *
 * <p>The map's tile layers are added to the world in the {@code BACKGROUND} pass, so they are drawn
 * in document order by the core render pipeline. The world camera is centred by {@link
 * BaseGameScreen} (the context is sized to the map), and this screen wires drag-to-pan and
 * scroll-to-zoom so the map can be explored.
 */
public final class CaveDemoScreen extends BaseGameScreen<DemoGameContext> {

  /** Classpath of the demo map (its TSX and PNG are resolved alongside it). */
  public static final String MAP = "assets/maps/Cave.tmx";

  private static final String SPRITE_TOWER = "assets/sprites/Tower.png";
  private static final String SPRITE_ENEMY = "assets/sprites/Enemy.png";
  private static final String SPRITE_BULLET = "assets/sprites/Bullet.png";

  /** {@code Cave_Tilemap.png} tile index 0 (top-left): the light-brown walkable floor. */
  private static final int FLOOR_GID = 1;

  private static final int RANGE_RING_DIAMETER_PX = 256;
  private static final float RANGE_RING_THICKNESS_PX = 6f;
  private static final Color RANGE_RING_COLOR = new Color(1f, 1f, 1f, 0.85f);

  private TowerDefenseController towerDefense;

  public CaveDemoScreen(DemoGameContext context) {
    super(context);
  }

  @Override
  protected Color clearColor() {
    return Color.BLACK;
  }

  @Override
  public void show() {
    TiledMapLoader loader = new TiledMapLoader(context.assets().resources());
    TiledMap map;
    try {
      map = loader.load(MAP);
    } catch (IOException e) {
      throw new GdxRuntimeException("Failed to load demo map: " + MAP, e);
    }
    map.addTo(world);

    Grid grid = TiledGrids.fromMap(map.model());
    List<TileLayer> tileLayers = map.model().tileLayers();
    if (tileLayers.isEmpty()) {
      throw new GdxRuntimeException("Demo map has no tile layer: " + MAP);
    }
    Board border = TiledBoards.fromLayer(map.model(), tileLayers.get(0), gid -> gid != FLOOR_GID);

    var resources = context.assets().resources();
    TextureRegion towerTexture =
        new TextureRegion(
            resources.createTexture(SPRITE_TOWER, TextureFilter.Linear, TextureFilter.Linear));
    TextureRegion enemyTexture =
        new TextureRegion(
            resources.createTexture(SPRITE_ENEMY, TextureFilter.Linear, TextureFilter.Linear));
    TextureRegion bulletTexture =
        new TextureRegion(
            resources.createTexture(SPRITE_BULLET, TextureFilter.Linear, TextureFilter.Linear));
    ShapeTextureFactory shapes = new ShapeTextureFactory(resources);
    TextureRegion rangeRingTexture =
        new TextureRegion(
            shapes.ring(RANGE_RING_DIAMETER_PX, RANGE_RING_THICKNESS_PX, RANGE_RING_COLOR));
    towerDefense =
        new TowerDefenseController(
            world,
            worldCamera,
            grid,
            border,
            towerTexture,
            enemyTexture,
            bulletTexture,
            rangeRingTexture);

    InputMultiplexer input =
        new InputMultiplexer(
            timeControlAdapter(),
            towerPlacementAdapter(),
            worldCamera.dragAdapter(),
            worldCamera.scrollAdapter());
    Gdx.input.setInputProcessor(input);
  }

  /**
   * Handles Space (pause toggle) and 1/2 (normal/double speed) via the inherited {@link
   * #timeScale}.
   */
  private InputAdapter timeControlAdapter() {
    return new InputAdapter() {
      @Override
      public boolean keyDown(int keycode) {
        switch (keycode) {
          case Input.Keys.SPACE:
            timeScale.togglePause();
            return true;
          case Input.Keys.NUM_1:
            timeScale.setScale(1f);
            return true;
          case Input.Keys.NUM_2:
            timeScale.setScale(2f);
            return true;
          default:
            return false;
        }
      }
    };
  }

  /**
   * Handles left-click tower placement. Placed before the drag adapter in the multiplexer so a
   * successful placement consumes the press and does not also start a camera drag.
   */
  private InputAdapter towerPlacementAdapter() {
    return new InputAdapter() {
      @Override
      public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.LEFT) return false;
        Vector3 clickWorldPos = worldCamera.unproject(screenX, screenY);
        return towerDefense.tryPlaceTower(clickWorldPos.x, clickWorldPos.y);
      }
    };
  }
}
