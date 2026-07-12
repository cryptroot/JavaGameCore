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
import com.cryptroot.core.screen.BaseGameScreen;
import com.cryptroot.demo.towerdefense.TowerDefenseController;
import com.cryptroot.tiled.render.TiledGrids;
import com.cryptroot.tiled.render.TiledMap;
import com.cryptroot.tiled.render.TiledMapLoader;
import java.io.IOException;

/**
 * Demo screen that loads {@code Cave.tmx} and renders it through the core world pipeline, with a
 * simple tower-defense mini-game layered on top: click the leftmost/rightmost tile columns to place
 * towers, which shoot enemies that spawn at the bottom of the map and path toward the top.
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
    towerDefense =
        new TowerDefenseController(world, grid, towerTexture, enemyTexture, bulletTexture);

    InputMultiplexer input =
        new InputMultiplexer(
            towerPlacementAdapter(), worldCamera.dragAdapter(), worldCamera.scrollAdapter());
    Gdx.input.setInputProcessor(input);
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
